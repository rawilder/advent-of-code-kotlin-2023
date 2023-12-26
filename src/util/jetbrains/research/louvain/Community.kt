package util.jetbrains.research.louvain

import kotlin.math.pow
import kotlin.math.sqrt

// https://github.com/JetBrains-Research/louvain

internal fun Community(nodeIndex: Int, graph: List<LouvainNode>): Community {
    val node = graph[nodeIndex]
    return Community(node.selfLoopsWeight, node.outDegree, mutableSetOf(nodeIndex))
}

internal class Community(
    private var selfLoopsWeight: Double = 0.0,
    private var outLinksWeight: Double = 0.0,
    val nodes: MutableSet<Int> = mutableSetOf()
) {

    private fun totalWeightsSum() = selfLoopsWeight + outLinksWeight

    fun addLouvainNode(index: Int, nodes: List<LouvainNode>) {
        val node = nodes[index]
        node.incidentLinks.forEach { link ->
            if (link.to in this.nodes) {
                selfLoopsWeight += 2 * link.weight
                outLinksWeight -= link.weight
            } else {
                outLinksWeight += link.weight
            }
        }
        selfLoopsWeight += node.selfLoopsWeight
        this.nodes.add(index)
    }

    fun removeLouvainNode(index: Int, nodes: List<LouvainNode>): Boolean {
        val node = nodes[index]
        node.incidentLinks.forEach { link ->
            if (link.to in this.nodes) {
                selfLoopsWeight -= 2 * link.weight
                outLinksWeight += link.weight
            } else {
                outLinksWeight -= link.weight
            }
        }
        this.nodes.remove(index)
        selfLoopsWeight -= node.selfLoopsWeight
        return this.nodes.size == 0
    }

    fun modularityChangeIfLouvainNodeAdded(node: LouvainNode, graphWeight: Double): Double =
        (1 / graphWeight) * (weightsToLouvainNode(node) - totalWeightsSum() * node.degree() / (2 * graphWeight))

    private fun weightsToLouvainNode(node: LouvainNode): Double = node.incidentLinks.filter { it.to in nodes }.sumOf { it.weight }

    fun computeModularity(graphWeight: Double): Double = (selfLoopsWeight / (2 * graphWeight)) - (totalWeightsSum() / (2 * graphWeight)).pow(2)

    fun toLouvainLouvainNode(nodes: List<LouvainNode>): LouvainNode {
        val newIndex = nodes[this.nodes.first()].community
        val consumedLouvainNodes = this.nodes.flatMap { nodes[it].originalLouvainNodes }.toSet()
        var newSelfLoopsWeight = 0.0

        val incidentLinksMap = mutableMapOf<Int, Double>()
        this.nodes.forEach { nodeIndex ->
            newSelfLoopsWeight += nodes[nodeIndex].selfLoopsWeight
            nodes[nodeIndex].incidentLinks.forEach { link ->
                val toNewLouvainNode = nodes[link.to].community
                if (toNewLouvainNode != newIndex) {
                    if (toNewLouvainNode in incidentLinksMap) {
                        incidentLinksMap[toNewLouvainNode] = incidentLinksMap[toNewLouvainNode]!! + link.weight
                    } else {
                        incidentLinksMap[toNewLouvainNode] = link.weight
                    }
                } else {
                    newSelfLoopsWeight += link.weight
                }
            }
        }
        val links = incidentLinksMap.map { InternalLink(it.key, it.value) }

        return LouvainNode(newIndex, consumedLouvainNodes, links, newSelfLoopsWeight)
    }

    /**
     * If communities size is less than sqrt(2 * graphWeight) then merging it with another one will always increase modularity.
     * Hence, if community size is greater than sqrt(2 * graphWeight), it might actually consist of several smaller communities.
     */
    fun overResolutionLimit(graphWeight: Double): Boolean = selfLoopsWeight >= sqrt(2 * graphWeight)
}
