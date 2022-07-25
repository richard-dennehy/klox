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

class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        var next = advance()
        while (next != null) {
            scanToken(next)
            start = current
            next = advance()
        }

        tokens.add(Token(EOF, "", line))
        return tokens.toList()
    }

    private fun scanToken(char: Char) {
        val token: Symbol = when (char) {
            '(' -> Symbol.LeftParenthesis
            ')' -> Symbol.RightParenthesis
            '[' -> Symbol.LeftBrace
            ']' -> Symbol.RightBrace
            ',' -> Symbol.Comma
            '.' -> Symbol.Dot
            '-' -> Symbol.Minus
            '+' -> Symbol.Plus
            ';' -> Symbol.Semicolon
            '*' -> Symbol.Asterisk
            '!' -> if (match('=')) {
                Symbol.NotEqual
            } else {
                Symbol.Not
            }
            '=' -> if (match('=')) {
                Symbol.DoubleEquals
            } else {
                Symbol.Equals
            }
            '>' -> if (match('=')) {
                Symbol.GreaterThanOrEqual
            } else {
                Symbol.GreaterThan
            }
            '<' -> if (match('=')) {
                Symbol.LessThanOrEqual
            } else {
                Symbol.LessThan
            }
            '/' -> if (match('/')) {
                while (peek() != '\n') advance()
                return
            } else {
                Symbol.Slash
            }
            ' ', '\r', '\t' -> return
            '\n' -> {
                line++
                return
            }
            else -> {
                error("Unexpected character.", line)
                return
            }
        }

        tokens.add(Token(token, source.substring(start, current), line))
    }

    private fun advance(): Char? = source.getOrNull(current++)

    private fun peek(): Char? = source.getOrNull(current)

    private fun match(expected: Char): Boolean {
        if (peek() != expected) return false

        advance()
        return true
    }
}

fun error(message: String, line: Int) {
    System.err.println("[line $line] Error: $message")
    hadError = true
}
