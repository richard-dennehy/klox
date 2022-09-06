import org.junit.jupiter.api.Test
import util.InterpreterTest

class ControlFlowTest: InterpreterTest() {

    @Test
    fun `basic if statement when condition is truthy`() {
        mustEvaluateTo("if (true) print 1;", "")
        mustHavePrinted("1")
    }

    @Test
    fun `basic if statement when condition is falsy`() {
        mustEvaluateTo("if (false) print 1;", "")
        mustHaveNotPrinted()
    }

    @Test
    fun `if statement with else branch when condition is truthy`() {
        mustEvaluateTo("if (true) print 1; else print 10;", "")
        mustHavePrinted("1")
    }

    @Test
    fun `if statement with else branch when condition is falsy`() {
        mustEvaluateTo("if (false) print 1; else print 10;", "")
        mustHavePrinted("10")
    }

    @Test
    fun `else branch associates with nearest if`() {
        mustEvaluateTo("if (true) if (false) print 1; else print 2;", "")
        mustHavePrinted("2")
    }

    @Test
    fun `if prints last statement in branch in REPL`() {
        mustEvaluateTo("if (true) 1; else -1;", "1")
    }

    @Test
    fun `or returns left operand if truthy`() {
        mustEvaluateTo("\"hi\" or 2;", "\"hi\"")
    }

    @Test
    fun `or returns right operand if left operand is falsy`() {
        mustEvaluateTo("nil or \"yes\";", "\"yes\"")
    }

    @Test
    fun `or does not evaluate right operand if left operand is truthy`() {
        mustEvaluateTo("fun debug(value) { print value; return value; } debug(true) or debug(false);", "true")
        mustHavePrinted("true")
    }

    @Test
    fun `and returns left operand if falsy`() {
        mustEvaluateTo("nil and \"yes\";", "nil")
    }

    @Test
    fun `and returns right operand if left operand is truthy`() {
        mustEvaluateTo("\"yes\" and nil;", "nil")
    }

    @Test
    fun `and does not evaluate right operand if left operand is falsy`() {
        mustEvaluateTo("fun debug(value) { print value; return value; } debug(false) and debug(true);", "false")
        mustHavePrinted("false")
    }

    @Test
    fun `while statement`() {
        mustEvaluateTo("var i = 5; while (i > 0) print i = i - 1;", "")
        mustHavePrinted("4", "3", "2", "1", "0")
    }

    @Test
    fun `while statement must have body`() {
        mustFailParsing("while (false)", "[line 1] Error at end: Expected expression.")
    }

    @Test
    fun `for statement`() {
        mustEvaluateTo("for (var i = 5; i > 0; i = i - 1) print i;", "")
        mustHavePrinted("5", "4", "3", "2", "1")
    }

    @Test
    fun `for statement initialiser is optional`() {
        mustEvaluateTo("var i = 5; for (; i > 0; i = i - 1) print i;", "")
        mustHavePrinted("5", "4", "3", "2", "1")
    }

    @Test
    fun `for statement initialiser does not need to declare a variable`() {
        mustEvaluateTo("var i; for (i = 5; i > 0; i = i - 1) print i;", "")
        mustHavePrinted("5", "4", "3", "2", "1")
    }

    @Test
    fun `for statement condition is optional`() {
        mustEvaluateTo("for (var i = 5; ; i = i - 1) { if (i <= 0) break; print i; }", "")
        mustHavePrinted("5", "4", "3", "2", "1")
    }

    @Test
    fun `for statement incrementer is optional`() {
        mustEvaluateTo("for (var i = 5; i > 0;) print i = i - 1;", "")
        mustHavePrinted("4", "3", "2", "1", "0")
    }

    @Test
    fun `breaking out of a while statement`() {
        mustEvaluateTo("while (true) break;", "")
    }

    @Test
    fun `breaking out of for loop`() {
        mustEvaluateTo("var i = 0; for (; i > 0; i = i + 1) break; i;", "0")
    }

    @Test
    fun `breaking out of nested blocks`() {
        val source = """var worked = "yes"; 
            |for (var i = 0; i < 10; i = i + 1) {
            |  if (true) {
            |    if (false) {
            |       worked = "no";
            |    } else {
            |       break;
            |       worked = "no";
            |    }
            |    worked = "no";
            |  }
            |  
            |  worked = "no";
            |} 
            |worked;""".trimMargin()
        mustEvaluateTo(source, "\"yes\"")
    }

    @Test
    fun `breaking out of nested loops`() {
        val source = """var i = 0; var j = 0;
            |for (; i < 5; i = i + 1) {
            |  for (;; j = j + 1) {
            |    if (j > 1) break;
            |  }
            |}
            |print i;
            |print j;
        """.trimMargin()
        mustEvaluateTo(source, "")
        mustHavePrinted("5", "2")
    }

    @Test
    fun `break cannot appear outside of loop`() {
        mustFailParsing("break;", "[line 1] Error at ';': 'break' not inside a loop.")
        mustFailParsing("if (true) break;", "[line 1] Error at ';': 'break' not inside a loop.")
        mustFailParsing("{ break; }", "[line 1] Error at ';': 'break' not inside a loop.")
    }
}
