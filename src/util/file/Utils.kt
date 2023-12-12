package util.file

import kotlin.io.path.Path
import kotlin.io.path.readLines

/**
 * Reads lines from the given input txt file.
 */
fun readInput(name: String) = Path("src/$name.txt").readLines()

/**
 * Reads lines from the given filename.
 */
fun readFile(name: String) = Path("src/$name").readLines()
