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

private val runner = Runner(SystemIO())

private fun runFile(path: String) {
    val result = runner.run(Files.readString(Paths.get(path)))
    result.print()

    when (result) {
        is RunResult.InterpreterError -> exitProcess(70)
        is RunResult.ParseError, is RunResult.ResolverError -> exitProcess(65)
        is RunResult.Success -> {}
    }
}

private fun runPrompt() {
    val reader = BufferedReader(InputStreamReader(System.`in`))

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        // keep running after errors
        runner.run(line).print()
    }
}

class Runner(io: IO) {
    private val interpreter = Interpreter(io)
    private val resolver = Resolver(interpreter)

    fun run(source: String): RunResult {
        val scanResult = scanTokens(source)

        return if (scanResult.errors.isEmpty()) {
            val parseResult = parse(scanResult.tokens)
            if (parseResult.errors.isEmpty()) {
                val resolvePass = resolver.resolve(parseResult.statements)
                if (resolvePass.errors.isEmpty()) {
                    when (val result = interpreter.interpret(parseResult.statements)) {
                        is InterpreterResult.Success -> RunResult.Success(result.data ?: "")
                        is InterpreterResult.Error -> {
                            RunResult.InterpreterError(result.message)
                        }
                    }
                } else {
                    RunResult.ResolverError(resolvePass.errors)
                }
            } else {
                RunResult.ParseError(scanResult.errors + parseResult.errors)
            }
        } else {
            RunResult.ParseError(scanResult.errors)
        }
    }
}

sealed interface RunResult {
    fun print()

    data class ParseError(val errors: List<String>) : RunResult {
        override fun print() = errors.forEach(System.err::println)
    }

    data class InterpreterError(val error: String) : RunResult {
        override fun print() = System.err.println(error)
    }

    data class ResolverError(val errors: List<String>): RunResult {
        override fun print() = errors.forEach(System.err::println)
    }

    data class Success(val data: String) : RunResult {
        override fun print() = println(data)
    }
}
