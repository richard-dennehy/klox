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
        var frozenTime: Double? = null

        override fun print(text: String) = printed.add(text).run {}
        override fun currentTime(): Double = frozenTime ?: (System.currentTimeMillis() / 1000.0)
    }

    private val testIO = TestIO()
    private val runner = Runner(testIO)

    @BeforeEach
    fun reset() {
        testIO.printed.clear()
        testIO.frozenTime = null
    }

    fun mustEvaluateTo(source: String, expected: String) {
        when(val result = runner.run(source)) {
            is RunResult.Success -> assertEquals(expected, result.data)
            else ->
                fail("Failed to interpret. \nSource\n$source\nResult:\n$result")
        }
    }

    fun mustFailParsing(source: String, expectedError: String) {
        when(val result = runner.run(source)) {
            is RunResult.ParseError ->
                assertEquals(expectedError, result.errors.joinToString("\n"), source)
            else ->
                fail("Expected parsing to fail, but it succeeded.\nSource:\n$source\nResult:\n$result")
        }
    }

    fun mustFailExecution(source: String, expectedError: String) {
        when(val result = runner.run(source)) {
            is RunResult.InterpreterError ->
                assertEquals(expectedError, result.error, source)
            is RunResult.Success ->
                fail("Expected interpreter to fail, but it succeeded.\nSource:\n$source\nResult:\n$result")
            else ->
                fail("Failed for unexpected reasons.\nSource:\n$source\nResult:\n$result")
        }
    }

    fun mustFailResolving(source: String, expectedError: String) {
        when (val result = runner.run(source)) {
            is RunResult.ResolverError ->
                assertEquals(expectedError, result.errors.joinToString("\n"))
            is RunResult.Success ->
                fail("Expected interpreter to fail, but it succeeded.\nSource:\n$source\nResult:\n$result")
            else ->
                fail("Failed for unexpected reasons.\nSource:\n$source\nResult:\n$result")
        }
    }

    fun mustHavePrinted(vararg messages: String) {
        assertEquals(messages.toList(), testIO.printed.toList())
    }

    fun mustHaveNotPrinted() {
        assertEquals(listOf(), testIO.printed.toList())
    }

    fun freezeTime(time: Double) {
        testIO.frozenTime = time
    }
}
