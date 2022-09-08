import util.InterpreterTest
import kotlin.test.Test

class FunctionsTest : InterpreterTest() {
    @Test
    fun `calling built-in function`() {
        freezeTime(1662466230.0)
        mustEvaluateTo("clock();", "1.66246623E9")
    }

    @Test
    fun `binding function to a variable`() {
        freezeTime(1662466230.0)
        mustEvaluateTo("var time = clock; time();", "1.66246623E9")
    }

    @Test
    fun `function with parameters`() {
        mustEvaluateTo(
            "fun sayHi(first, last) { print \"Hi, \" + first + \" \" + last + \"!\"; } sayHi(\"Dear\", \"Reader\");",
            "nil"
        )
        mustHavePrinted("Hi, Dear Reader!")
    }

    @Test
    fun `returning from function`() {
        mustEvaluateTo("fun count(n) { while (n < 100) { if (n == 3) return n; print n; n = n + 1; } } count(1);", "3")
        mustHavePrinted("1", "2")
    }

    @Test
    fun `returning without a value`() {
        mustEvaluateTo("fun explicitReturn() { return; } explicitReturn();", "nil")
    }

    @Test
    fun `taking value of non-returning function`() {
        mustEvaluateTo("fun noop() {} var returned = noop(); returned;", "nil")
    }

    @Test
    fun `recursive function`() {
        mustEvaluateTo(
            "fun fib(n) { if (n <= 1) return n; return fib(n - 2) + fib(n - 1); } for (var i = 0; i < 20; i = i + 1) { print fib(i); }",
            ""
        )
        mustHavePrinted(
            "0",
            "1",
            "1",
            "2",
            "3",
            "5",
            "8",
            "13",
            "21",
            "34",
            "55",
            "89",
            "144",
            "233",
            "377",
            "610",
            "987",
            "1597",
            "2584",
            "4181"
        )
    }

    @Test
    fun closure() {
        mustEvaluateTo(
            "fun makeCounter() { var i = 0; fun count() { i = i + 1; print i; } return count; } var counter = makeCounter(); counter(); counter();",
            "nil"
        )
        mustHavePrinted("1", "2")
    }

    @Test
    fun `anonymous function`() {
        val source = """fun thrice(fn) {
  for (var i = 1; i <= 3; i = i + 1) {
    fn(i);
  }
}

thrice(fun (a) {
  print a;
});"""
        mustEvaluateTo(source, "nil")
        mustHavePrinted("1", "2", "3")
    }

    @Test
    fun `assigning anonymous function to variable`() {
        mustEvaluateTo("var p = fun (a) { print a; }; p(10);", "nil")
        mustHavePrinted("10")
    }

    @Test
    fun `passing the wrong number of arguments`() {
        mustFailExecution("fun tooManyArgs() {} tooManyArgs(1, 2, 3);", "Expected 0 arguments but got 3.\n[line 1]")
        mustFailExecution(
            "fun notEnoughArgs(first, second, third) {} notEnoughArgs(1);", "Expected 3 arguments but got 1.\n[line 1]"
        )
    }

    @Test
    fun `passing more than 255 arguments`() {
        val source = "fun someFunction() {} someFunction(${(0..255).joinToString(", ") { "arg$it" }});"
        mustFailParsing(source, "[line 1] Error at 'arg255': Can't have more than 255 arguments.")
    }

    @Test
    fun `declaring function with more than 255 parameters`() {
        val source = "fun someFunction(${(0..255).joinToString(", ") { "param$it" }}) {}"
        mustFailParsing(source, "[line 1] Error at 'param255': Can't have more than 255 parameters.")
    }
}
