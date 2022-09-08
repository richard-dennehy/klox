import util.InterpreterTest
import kotlin.test.Test

class ResolverTest: InterpreterTest() {
    @Test
    fun `declaring a variable twice in non-local scope`() {
        mustFailResolving("{var a = 1; var a = 2;}", "[line 1] Error a: Already a variable with this name in this scope.")
    }

    @Test
    fun `return outside a function`() {
        mustFailResolving("return nil;", "[line 1] Error return: Can't return from top-level code.")
    }
}
