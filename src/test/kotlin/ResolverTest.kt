import util.InterpreterTest
import kotlin.test.Test

class ResolverTest: InterpreterTest() {
    @Test
    fun `declaring a variable twice in non-local scope`() {
        mustFailResolving("{var a = 1; var a = 2; print a;}", "[line 1] Error at a: Already a variable with this name in this scope.")
    }

    @Test
    fun `return outside a function`() {
        mustFailResolving("return nil;", "[line 1] Error at return: Can't return from top-level code.")
    }

    @Test
    fun `unused local variable`() {
        mustFailResolving("{var a = 1;}", "[line 1] Error at a: Variable is never used.")
    }

    @Test
    fun `block declared variables are dropped at the block end`() {
        mustFailResolving("{ var a = 1; } print a;", "[line 1] Error at a: Variable is never used.")
    }
}
