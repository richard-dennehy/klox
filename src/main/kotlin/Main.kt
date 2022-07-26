import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

var hadError = false

fun runFile(path: String) {
    run(Files.readString(Paths.get(path)))

    if (hadError) exitProcess(65)
}

fun runPrompt() {
    val reader = BufferedReader(InputStreamReader(System.`in`))

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run(line)
        hadError = false
    }
}

fun run(source: String) {
    val tokens = Scanner(source).scanTokens()

    for (token in tokens) {
        println(token)
    }
}

fun reportError(message: String, line: Int) {
    System.err.println("[line $line] Error: $message")
    hadError = true
}

