import org.junit.jupiter.api.Test
import util.InterpreterTest
import kotlin.test.Ignore

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
    @Ignore("Can't demonstrate this until functions are implemented")
    fun `or does not evaluate right operand if left operand is truthy`() {
        TODO()
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
    @Ignore("Can't demonstrate this until functions are implemented")
    fun `and does not evaluate right operand if left operand is falsy`() {
        TODO()
    }

    @Test
    fun `while statement`() {
        mustEvaluateTo("var i = 5; while (i > 0) print i = i - 1;", "")
        mustHavePrinted("4", "3", "2", "1", "0")
    }

    @Test
    fun `while prints result of last statement in REPL`() {
        mustEvaluateTo("var i = 5; while (i > 0) i = i - 1;", "0")
    }

    @Test
    fun `while statement must have body`() {
        mustFailParsing("while (false)", "[line 1] Error at end: Expected expression.")
    }

    @Test
    fun `for statement`() {
        mustEvaluateTo("for (var i = 5; i > 0; i = i - 1) print i;", "0")
        mustHavePrinted("5", "4", "3", "2", "1")
    }
}
