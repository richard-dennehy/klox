import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class InterpreterTest {
    private fun RunResult.stringified(): String {
        return when (this) {
            is RunResult.InterpreterError -> this.error
            is RunResult.ParseError -> this.errors.joinToString("\n")
            is RunResult.Success -> this.data
        }
    }

    private fun mustEvaluateTo(source: String, result: String) {
        assertEquals(result, run(source).stringified(), source)
    }

    private fun mustFailParsing(source: String, expectedError: String) {
        when(val result = run(source)) {
            is RunResult.ParseError ->
                assertEquals(expectedError, result.stringified(), source)
            else ->
                fail("Expected parsing to fail, but it succeeded. Source:\n$source\nResult: $result")
        }
    }

    private fun mustFailExecution(source: String, expectedError: String) {
        when(val result = run(source)) {
            is RunResult.InterpreterError ->
                assertEquals(expectedError, result.stringified(), source)
            else ->
                fail("Expected interpreter to fail, but it succeeded. Source:\n$source\nResult: $result")
        }
    }

    @Test
    fun `nil literal`() {
        mustEvaluateTo("nil", "nil")
    }

    @Test
    fun `true literal`() {
        mustEvaluateTo("true", "true")
    }

    @Test
    fun `false literal`() {
        mustEvaluateTo("false", "false")
    }

    @Test
    fun `negated nil`() {
        mustEvaluateTo("!nil", "true")
    }

    @Test
    fun `negated true`() {
        mustEvaluateTo("!true", "false")
    }

    @Test
    fun `negated false`() {
        mustEvaluateTo("!false", "true")
    }

    @Test
    fun `empty string literal`() {
        mustEvaluateTo("\"\"", "\"\"")
    }

    @Test
    fun `non empty string literal`() {
        val value = "\"something something dark side\""
        mustEvaluateTo(value, value)
    }

    @Test
    fun `negated string literal`() {
        mustEvaluateTo("!\"\"", "false")
    }

    @Test
    fun `string concatenation`() {
        mustEvaluateTo("\"something something\" + \" dark side\"", "\"something something dark side\"")
    }

    @Test
    fun `integer literal`() {
        mustEvaluateTo("12345", "12345")
    }

    @Test
    fun `negated integer literal`() {
        mustEvaluateTo("!0", "false")
    }

    @Test
    fun `real number literal`() {
        mustEvaluateTo("123.456", "123.456")
    }

    @Test
    fun `negated real number literal`() {
        mustEvaluateTo("!0.0", "false")
    }

    @Test
    fun `negative integer literal`() {
        mustEvaluateTo("-987", "-987")
    }

    @Test
    fun `negative real number literal`() {
        mustEvaluateTo("-987.654", "-987.654")
    }

    @Test
    fun `numeric addition`() {
        mustEvaluateTo("1 + 2.45", "3.45")
    }

    @Test
    fun `numeric subtraction`() {
        mustEvaluateTo("7 - 8", "-1")
    }

    @Test
    fun `numeric multiplication`() {
        mustEvaluateTo("5 * 6", "30")
    }

    @Test
    fun `numeric division`() {
        mustEvaluateTo("2 / 4", "0.5")
    }

    @Test
    fun `order of mathematical operations`() {
        mustEvaluateTo("2 * 3 + 4 / 5 - 10", "-3.2")
    }

    @Test
    fun `whitespace is ignored`() {
        mustEvaluateTo("1+2", "3")
        mustEvaluateTo("                                     1                                      + \t\r      2", "3")
    }

    @Test
    fun `numeric comparison`() {
        arrayOf(
            // operator | smaller value | same value | larger value
            ">" to arrayOf(true, false, false),
            ">=" to arrayOf(true, true, false),
            "<" to arrayOf(false, false, true),
            "<=" to arrayOf(false, true, true),
        ).forEach {
            mustEvaluateTo("5 ${it.first} 4", "${it.second[0]}")
            mustEvaluateTo("5 ${it.first} 5", "${it.second[1]}")
            mustEvaluateTo("5 ${it.first} 6", "${it.second[2]}")
        }
    }

    @Test
    fun `parenthesised expression`() {
        mustEvaluateTo("(\"something something dark side\")", "\"something something dark side\"")
    }

    @Test
    fun `parentheses change order of mathematical operations`() {
        mustEvaluateTo("2 * (3 + 4) / (5 - 10)", "-2.8")
    }

    @Test
    fun `deeply nested parentheses`() {
        mustEvaluateTo("((((((((((((((-((((1))))))) + ((((((((((4))))) * 5))))))))))))))) / ((((2)))))", "9.5")
    }

    private fun mustBeEqual(left: String, right: String) {
        mustEvaluateTo("$left == $right", "true")
        mustEvaluateTo("$left != $right", "false")
    }

    private fun mustNotBeEqual(left: String, right: String) {
        mustEvaluateTo("$left == $right", "false")
        mustEvaluateTo("$left != $right", "true")
    }

    @Test
    fun `different types are never equal`() {
        arrayOf("\"0\"", "false", "nil", "true").forEach {
            mustNotBeEqual("0", it)
        }
    }

    @Test
    fun `numeric equality`() {
        mustBeEqual("0", "0")
        mustBeEqual("0", "0.0")
        mustBeEqual("123456.789", "123456.789")
        mustBeEqual("123", "1 * 100 + 2 * 10 + 3")
    }

    @Test
    fun `boolean equality`() {
        mustBeEqual("true", "true")
        mustNotBeEqual("true", "false")
        mustBeEqual("false", "false")
        mustNotBeEqual("false", "true")
        mustBeEqual("true", "!false")
        mustBeEqual("false", "!true")
    }

    @Test
    fun `nil equality`() {
        mustBeEqual("nil", "nil")
    }

    @Test
    fun `string equality`() {
        mustBeEqual("\"\"", "\"\"")
        mustBeEqual("\"words words words\"", "\"words words words\"")
        mustBeEqual("\"words words\" + \" words\"", "\"words\" + \" \" + \"words\" + \" \" + \"words\"")
    }

    @Test
    fun `multi-line equality`() {
        val source = """1 + 1
            |==
            |2
        """.trimMargin()
        mustEvaluateTo(source, "true")
    }

    @Test
    fun `double negation`() {
        mustEvaluateTo("!!nil", "false")
    }

    @Test
    fun `double negative`() {
        mustEvaluateTo("--1", "1")
    }

    @Test
    fun `line comment`() {
        val source = """
            // incredibly helpful comment explaining how this adds one to two
            1 + 2
        """.trimIndent()
        mustEvaluateTo(source, "3")
    }

    @Test
    fun `block comment`() {
        val source = """
            /* 
            a very long and detailed explanation of all the things
            that the very complicated
            code that follows does
            */
            0.5 * 2
        """.trimIndent()
        mustEvaluateTo(source, "1")
    }

    @Test
    fun `nested block comment`() {
        val source = """
            /*
            a helpful comment that
                /*
                gets rudely interrupted by a different block comment
                */
            explains what the following code does
            */
            2 / 0.5
        """.trimIndent()
        mustEvaluateTo(source, "4")
    }

    @Test
    fun `block comment in an expression`() {
        mustEvaluateTo("1 /* hi I'm a helpful comment */ + 2", "3")
    }

    @Test
    fun `multi-line expression`() {
        val source = """
            2 * (
                3 + 4) 
            / 
            (
                5 - 
            10
            
            )
        """.trimIndent()

        mustEvaluateTo(source, "-2.8")
    }

    @Test
    fun `adding string to non-string`() {
        arrayOf(
            "1",
            "false",
            "true",
            "nil"
        ).forEach {
            mustEvaluateTo("\"stringified: \" + $it", "\"stringified: $it\"")
            mustEvaluateTo("$it + \", stringified\"", "\"$it, stringified\"")
        }
    }

    @Test
    fun `cannot add numeric and non-numeric`() {
        arrayOf(
            "false",
            "true",
            "nil"
        ).forEach {
            mustFailExecution(
                "2 + $it", "Operand must be a string or a number\n[line 1]"
            )
        }
    }

    @Test
    fun `cannot add booleans or nils`() {
        mustFailExecution(
            "true + false", "Operand must be a string or a number\n[line 1]"
        )
        mustFailExecution(
            "nil + nil", "Operand must be a string or a number\n[line 1]"
        )
        mustFailExecution(
            "nil + true", "Operand must be a string or a number\n[line 1]"
        )
    }

    @Test
    fun `cannot use mathematical operators on non-numeric values`() {
        val operators = arrayOf(
            "-",
            "/",
            "*",
            ">",
            ">=",
            "<",
            "<=",
        )
        val values = arrayOf(
            "true",
            "false",
            "nil",
            "\"a string\""
        )
        values.forEach { first ->
            values.forEach { second ->
                operators.forEach { op ->
                    mustFailExecution("$first $op $second", "Operand must be a number\n[line 1]")
                }
            }
        }
    }

    @Test
    fun `unexpected symbol`() {
        val source = """??
            |?£
            |⅛^^^
            |1""".trimMargin()
        mustFailParsing(source, "[line 1] Error: Unexpected character.\n" +
                "[line 1] Error: Unexpected character.\n" +
                "[line 2] Error: Unexpected character.\n" +
                "[line 2] Error: Unexpected character.\n" +
                "[line 3] Error: Unexpected character.")
    }

    @Test
    fun `binary operator missing second argument`() {
        arrayOf("+", "/", "-", "*", ">", ">=", "<", "<=").forEach {
            mustFailParsing("1 $it", "[line 1] Error at end: Expected expression.")
        }
    }

    @Test
    fun `binary operator used as unary operator`() {
        arrayOf("+", "/", "*", ">", ">=", "<", "<=").forEach {
            mustFailParsing("$it 1", "[line 1] Error at '$it': Expected expression.")
        }
    }

    @Test
    fun `unterminated string`() {
        mustFailParsing(" \"oh no", "[line 1] Error: Unterminated string.")
    }

    @Test
    fun `unterminated block comment`() {
        mustFailParsing("/*oh no", "[line 1] Error: Unterminated block comment.")
    }

    @Test
    fun `unterminated nested block comment`() {
        val source = """/*oh no
            |/*oh no
            |/*oh no no no
        """.trimMargin()
        mustFailParsing(source, "[line 3] Error: Unterminated block comment.")
    }

    @Test
    fun `empty input`() {
        mustFailParsing("", "[line 1] Error at end: Expected expression.")
    }

    @Test
    fun `division by zero`() {
        mustFailExecution("1 / (1 - 1)", "Division by zero\n[line 1]")
    }
}
