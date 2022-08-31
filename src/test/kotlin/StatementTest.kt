import org.junit.jupiter.api.Test
import util.InterpreterTest

class StatementTest: InterpreterTest() {
    @Test
    fun `uninitialised variable declaration`() {
        mustEvaluateTo("var a; a;", "nil")
    }

    @Test
    fun `variable declaration using literal value`() {
        mustEvaluateTo("var a = 1; a;", "1")
    }

    @Test
    fun `variable declaration using expression result`() {
        mustEvaluateTo("var a = 2 + 3; a;", "5")
    }

    @Test
    fun `initialising uninitialised variable`() {
        mustEvaluateTo("var a; a = 1; a;", "1")
    }

    @Test
    fun `initialising variable using another variable`() {
        mustEvaluateTo("var a = 0; var b = a; b;", "0")
    }

    @Test
    fun `using variable in expression`() {
        mustEvaluateTo("var a = 10; 5 * a;", "50")
    }

    @Test
    fun `printing literal value`() {
        mustEvaluateTo("print 1;", "")
        mustHavePrinted("1")
    }

    @Test
    fun `printing expression result`() {
        mustEvaluateTo("print 2 + 3;", "")
        mustHavePrinted("5")
    }

    @Test
    fun `printing initialised variable`() {
        mustEvaluateTo("var a = 1; print a;", "")
        mustHavePrinted("1")
    }

    @Test
    fun `compound assignment`() {
        mustEvaluateTo("var a; var b; a = b = 1; print a; print b;", "")
        mustHavePrinted("1", "1")
    }

    @Test
    fun `empty block`() {
        mustEvaluateTo("{}", "")
    }

    @Test
    fun `declaring variables inside block`() {
        mustEvaluateTo("{ var a = 0; a; }", "0")
    }

    @Test
    fun `referencing variables from outer scope`() {
        mustEvaluateTo("var a = 10; { 5 * a; }", "50")
    }

    @Test
    fun `shadowing variables`() {
        mustEvaluateTo("var a = 5; { var a = 10; a + 1; }", "11")
    }
}
