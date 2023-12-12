import util.file.readFile
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeLines

fun main() {
    // get day of month est since that's when aoc problems are released
    val dayOfMonth = Instant.now().atOffset(ZoneOffset.of("-05:00")).dayOfMonth
    val dayOfMonthString = dayOfMonth.toString().padStart(2, '0')
    createKotlinFile(dayOfMonthString)
    createTestInputs(dayOfMonthString)
    createInput(dayOfMonthString)
    println("Right click src -> Reload from disk to see new files if any were created.")
}

fun createKotlinFile(dayOfMonthString: String) {
    // ensure file doesn't exist
    ifDoesntExist("src/Day${dayOfMonthString}.kt") {
        // create file
        val newFile = Path("src/Day${dayOfMonthString}.kt").createFile()
        // write template to file
        newFile.writeLines(
            readFile("Day##.kt").map {
                if (it.contains("Day##")) {
                    it.replace("Day##", "Day${dayOfMonthString}")
                } else {
                    it
                }
            }
        )

        println("src/Day${dayOfMonthString}.kt created.")
    }
}

fun createTestInputs(dayOfMonthString: String) {
    val part1PathName = "src/Day${dayOfMonthString}_part1_test.txt"
    ifDoesntExist(part1PathName) {
        Path(part1PathName).createFile()
        println("$part1PathName created.")
    }
    val part2PathName = "src/Day${dayOfMonthString}_part2_test.txt"
    ifDoesntExist(part2PathName) {
        Path(part2PathName).createFile()
        println("$part2PathName created.")
    }
}

fun createInput(dayOfMonthString: String) {
    val pathName = "src/Day${dayOfMonthString}.txt"
    ifDoesntExist(pathName) {
        Path(pathName).createFile()
        println("$pathName created.")
    }
}

fun ifDoesntExist(filename: String, block: () -> Unit) {
    if (!Path(filename).exists()) {
        block()
    } else {
        println("File $filename already exists.")
    }
}
