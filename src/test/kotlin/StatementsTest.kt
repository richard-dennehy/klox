import org.junit.jupiter.api.Test
import util.InterpreterTest

class StatementsTest: InterpreterTest() {
    @Test
    fun `variable declaration using literal value`() {
        mustEvaluateTo("var a = 1; a;", "1")
    }

    @Test
    fun `variable name containing digit and underscore`() {
        mustEvaluateTo("var a1_ = 1; a1_;", "1")
    }

    @Test
    fun `variable declaration using expression result`() {
        mustEvaluateTo("var a = 2 + 3; a;", "5")
    }

    @Test
    fun `redeclaring variable`() {
        mustEvaluateTo("var a = 1; var a = 2; a;", "2")
    }

    @Test
    fun `variables persist between run calls (for REPL)`() {
        mustEvaluateTo("var a = 1; a;", "1")
        mustEvaluateTo("a;", "1")
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

    @Test
    fun `cannot assign to undeclared variable`() {
        mustFailExecution("a = 1;", "Undefined variable `a`.\n[line 1]")
    }

    @Test
    fun `cannot print nothing`() {
        mustFailParsing("print;", "[line 1] Error at ';': Expected expression.")
    }

    @Test
    fun `cannot print uninitialised variable`() {
        mustFailExecution("var uninitialised; print uninitialised;", "Uninitialised variable `uninitialised`\n[line 1]")
    }

    @Test
    fun `cannot reference uninitialised variable`() {
        mustFailExecution("var uninitialised; print 1 + uninitialised;", "Uninitialised variable `uninitialised`\n[line 1]")
    }

    @Test
    fun `interpreter continues parsing after invalid statements`() {
        mustFailParsing("var missingSemicolon = 1 print missingSemicolon; +notUnary", "[line 1] Error at 'print': Expect ';' after variable declaration\n" +
                "[line 1] Error at '+': Expected expression.")
    }
}
