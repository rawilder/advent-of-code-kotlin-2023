package util.jetbrains.research.louvain

// https://github.com/JetBrains-Research/louvain

interface Link {
    fun source(): Int
    fun target(): Int
    fun weight(): Double
}
