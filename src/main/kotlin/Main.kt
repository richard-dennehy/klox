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
var hadRuntimeError = false
val interpreter = Interpreter()

fun runFile(path: String) {
    run(Files.readString(Paths.get(path)))

    if (hadError) exitProcess(65)
    if (hadRuntimeError) exitProcess(70)
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
    val parser = Parser(tokens)
    val expression = parser.parse()

    if (expression == null) {
        if (hadError) {
            return
        } else {
            System.err.println("Expression did not parse, but no error was reported")
            return
        }
    }

    println(interpreter.interpret(expression))
}

fun reportParseError(message: String, line: Int, where: String = "") {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun reportParseError(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
        reportParseError(message, token.line, " at end")
    } else {
        reportParseError(message, token.line, " at'${token.lexeme}'")
    }
}

fun reportRuntimeError(error: LoxRuntimeError) {
    System.err.println("${error.message}\n[line ${error.token.line}]")
    hadRuntimeError = true
}
