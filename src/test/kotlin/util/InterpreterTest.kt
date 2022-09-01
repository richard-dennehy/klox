package util

import IO
import RunResult
import Runner
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class InterpreterTest {
    class TestIO: IO {
        val printed = mutableListOf<String>()

        override fun print(text: String) = printed.add(text).run {}
    }

    val testIO = TestIO()
    val runner = Runner(testIO)

    @BeforeEach
    fun reset() {
        testIO.printed.clear()
    }

    fun RunResult.stringified(): String {
        return when (this) {
            is RunResult.InterpreterError -> this.error
            is RunResult.ParseError -> this.errors.joinToString("\n")
            is RunResult.Success -> this.data
        }
    }

    fun mustEvaluateTo(source: String, result: String) {
        assertEquals(result, runner.run(source).stringified(), source)
    }

    fun mustFailParsing(source: String, expectedError: String) {
        when(val result = runner.run(source)) {
            is RunResult.ParseError ->
                assertEquals(expectedError, result.stringified(), source)
            else ->
                fail("Expected parsing to fail, but it succeeded.\nSource:\n$source\nResult:\n$result")
        }
    }

    fun mustFailExecution(source: String, expectedError: String) {
        when(val result = runner.run(source)) {
            is RunResult.InterpreterError ->
                assertEquals(expectedError, result.stringified(), source)
            is RunResult.ParseError ->
                fail("Unexpectedly failed parsing.\nSource:\n$source\nResult:\n$result")
            is RunResult.Success ->
                fail("Expected interpreter to fail, but it succeeded.\nSource:\n$source\nResult:\n$result")
        }
    }

    fun mustHavePrinted(vararg messages: String) {
        assertEquals(testIO.printed.toList(), messages.toList())
    }
}
