package util.jetbrains.research.louvain

// https://github.com/JetBrains-Research/louvain

/**
 * Class that encapsulates the Louvain algorithm.
 */
internal class Louvain(
    private val links: List<Link>
) {
    private var communities: MutableMap<Int, Community>
    private var nodes: List<LouvainNode> = emptyList()
    private var graphWeight: Double
    private val originalLouvainNodesNumber: Int

    init {
        buildLouvainNodesFromLinks()
        originalLouvainNodesNumber = nodes.size
        communities = nodes.withIndex().associate { it.index to Community(it.index, nodes) }.toMutableMap()
        graphWeight = computeGraphWeight()
    }

    private fun buildLouvainNodesFromLinks() {
        val nodeIndices = links.flatMap { listOf(it.source(), it.target()) }.distinct().sorted()
        val mutableLouvainNodes = nodeIndices
            .withIndex()
            .associateBy({ it.value }, { MutableLouvainNode(it.index, setOf(it.value)) })
        links.forEach { link ->
            if (link.source() == link.target()) {
                mutableLouvainNodes[link.source()]!!.selfLoopsWeight += 2 * link.weight()
            } else {
                val newSource = mutableLouvainNodes[link.source()]!!.community
                val newTarget = mutableLouvainNodes[link.target()]!!.community
                mutableLouvainNodes[link.source()]!!.incidentLinks.add(InternalLink(newTarget, link.weight()))
                mutableLouvainNodes[link.target()]!!.incidentLinks.add(InternalLink(newSource, link.weight()))
            }
        }
        nodes = mutableLouvainNodes.values.map { it.toLouvainNode() }
    }

    private fun computeGraphWeight() =
        nodes.sumOf { n -> n.incidentLinks.sumOf { l -> l.weight } + n.selfLoopsWeight } / 2

    private fun aggregateCommunities() {
        // re-index communities in nodes
        communities.values.withIndex().forEach { (newIndex, community) ->
            community.nodes.forEach { nodeIndex ->
                nodes[nodeIndex].community = newIndex
            }
        }

        val newLouvainNodes = communities.values.map { it.toLouvainLouvainNode(nodes) }
        val newCommunities =
            newLouvainNodes.withIndex().associateBy({ it.index }, { Community(it.index, nodes) }).toMutableMap()

        nodes = newLouvainNodes
        communities = newCommunities
    }

    private fun moveLouvainNode(nodeIndex: Int, node: LouvainNode, toCommunityIndex: Int) {
        val from = communities[node.community]!!
        if (from.removeLouvainNode(nodeIndex, nodes)) {
            communities.remove(node.community)
        }
        node.community = toCommunityIndex
        communities[toCommunityIndex]!!.addLouvainNode(nodeIndex, nodes)
    }

    private fun computeCostOfMovingOut(index: Int, node: LouvainNode): Double {
        val theCommunity = communities[node.community]!!
        theCommunity.removeLouvainNode(index, nodes)
        val cost = theCommunity.modularityChangeIfLouvainNodeAdded(node, graphWeight)
        theCommunity.addLouvainNode(index, nodes)
        return cost
    }

    /**
     * Step I of the algorithm:
     * For each node i evaluate the gain in modularity if node i is moved to the community of one of its neighbors j.
     * Then move node i in the community for which the modularity gain is the largest, but only if this gain is positive.
     * This process is applied to all nodes until no further improvement can be achieved, completing Step I.
     * @see optimizeModularity
     */
    private fun findLocalMaxModularityPartition() {
        var repeat = true
        while (repeat) {
            repeat = false
            for ((i, node) in nodes.withIndex()) {
                var bestCommunity = node.community
                var maxDeltaM = 0.0
                val costOfMovingOut = computeCostOfMovingOut(i, node)
                for (communityIndex in node.neighbourCommunities(nodes)) {
                    if (communityIndex == node.community) {
                        continue
                    }
                    val toCommunity = communities[communityIndex]!!
                    val deltaM = toCommunity.modularityChangeIfLouvainNodeAdded(node, graphWeight) - costOfMovingOut
                    if (deltaM > maxDeltaM) {
                        bestCommunity = communityIndex
                        maxDeltaM = deltaM
                    }
                }
                if (bestCommunity != node.community) {
                    moveLouvainNode(i, node, bestCommunity)
                    repeat = true
                }
            }
        }
    }

    private fun computeModularity() = communities.values.sumOf { it.computeModularity(graphWeight) }

    fun optimizeModularity(depth: Int = 0) {
        var bestModularity = computeModularity()
        var bestCommunities = communities
        var bestLouvainNodes = nodes
        do {
            val from = communities.size
            findLocalMaxModularityPartition()
            aggregateCommunities()
            val newModularity = computeModularity()
            if (newModularity > bestModularity) {
                bestModularity = newModularity
                bestCommunities = communities
                bestLouvainNodes = nodes
            }
        } while (communities.size != from)
        communities = bestCommunities
        nodes = bestLouvainNodes
        if (communities.size != 1 && depth != 0) {
            refine(depth)
        }
    }

    fun resultingCommunities(): Map<Int, Int> {
        val communitiesMap = mutableMapOf<Int, Int>()
        communities.forEach { (communityIndex, community) ->
            community.nodes.forEach { nodeIndex ->
                val node: LouvainNode = nodes[nodeIndex]
                node.originalLouvainNodes.forEach {
                    communitiesMap[it] = communityIndex
                }
            }
        }
        return communitiesMap
    }

    private fun assignCommunities(communitiesMap: Map<Int, Int>) {
        communities.clear()
        buildLouvainNodesFromLinks()
        buildCommunitiesFromMap(communitiesMap)
    }

    private fun buildCommunitiesFromMap(communitiesMap: Map<Int, Int>) {
        // create all necessary communities
        for (entry in communitiesMap) {
            val communityIndex = entry.value
            if (communityIndex !in communities.keys) {
                communities[communityIndex] = Community()
            }
        }

        val nodeIndicesMap = communitiesMap.keys.sorted().withIndex().associateBy({ it.value }, { it.index })

        // distribute the nodes among communities
        for (entry in communitiesMap) {
            val nodeIndex = nodeIndicesMap[entry.key]!!
            val communityIndex = entry.value

            nodes[nodeIndex].community = -1
            communities[communityIndex]!!.addLouvainNode(nodeIndex, nodes)
            nodes[nodeIndex].community = communityIndex
        }
    }

    private fun refine(depth: Int = 0) {
        var communitiesMap = resultingCommunities()
        var resultingCommunitiesNumber = communitiesMap.values.distinct().size
        links
            .filter { communitiesMap[it.source()] == communitiesMap[it.target()] }
            .groupBy({ communitiesMap[it.source()]!! }, { it })
            .filter { communities[it.key]!!.overResolutionLimit(graphWeight) }
            .forEach { (communityIndex, links) ->
                val thisLouvain = Louvain(links)
                thisLouvain.optimizeModularity(depth - 1)
                val thisMap = thisLouvain.resultingCommunities()
                val reindex = reIndexMap(thisMap, communityIndex, resultingCommunitiesNumber)
                communitiesMap = communitiesMap + reindex
                resultingCommunitiesNumber = communitiesMap.values.distinct().size
            }
        assignCommunities(communitiesMap)
    }

    private fun reIndexMap(theMap: Map<Int, Int>, saveIndex: Int, startFrom: Int) = theMap
        .mapValues { (_, communityIndex) ->
            if (communityIndex == 0) {
                saveIndex
            } else {
                communityIndex + startFrom - 1
            }
        }
}
