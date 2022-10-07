import util.InterpreterTest
import kotlin.test.Test

class InheritanceTest: InterpreterTest() {
    @Test
    fun `simple inheritance`() {
        val source = """
            class Doughnut {
              cook() {
                print "Fry until golden brown.";
              }
            }

            class BostonCream < Doughnut {}

            BostonCream().cook();
        """.trimIndent()

        mustEvaluateTo(source, "nil")
        mustHavePrinted("Fry until golden brown.")
    }

    @Test
    fun `calling inherited method using super`() {
        val source = """
            class A {
              method() {
                print "A method";
              }
            }

            class B < A {
              method() {
                print "B method";
              }

              test() {
                super.method();
              }
            }

            class C < B {}

            C().test();
        """.trimIndent()

        mustEvaluateTo(source, "nil")
        mustHavePrinted("A method")
    }

    @Test
    fun `calling method using this inside super method`() {
        val source = """
            class A {
              first() {
                return this.second();
              }
            }

            class B < A {
              first() {
                return super.first();
              }

              second() {
                return "this is kind of weird but ok";
              }
            }

            B().first();
        """.trimIndent()

        mustEvaluateTo(source, "\"this is kind of weird but ok\"")
    }

    @Test
    fun `inheriting from self`() {
        val source = "class Oops < Oops {}"
        mustFailResolving(source, "[line 1] Error at `Oops`: A class can't inherit from itself.")
    }

    @Test
    fun `inheriting from non-class`() {
        val source = """
            var NotAClass = "I am totally not a class";

            class Subclass < NotAClass {}
        """.trimIndent()
        mustFailExecution(source, "Superclass must be a class.\n[line 3]")
    }

    @Test
    fun `using super in a class without a superclass`() {
        val source = """
            class Eclair {
              cook() {
                super.cook();
                print "Pipe full of crème pâtissière.";
              }
            }
        """.trimIndent()

        mustFailResolving(source, "[line 3] Error at `super`: Can't use `super` in a class with no superclass.")
    }

    @Test
    fun `using super outside a class`() {
        mustFailResolving("super.notEvenInAClass();", "[line 1] Error at `super`: Can't use `super` outside a class.")
    }

    @Test
    fun `using super in a static class method`() {
        val source = """
            class A {
              method() {
                this.shouldNotHappen;
              }
            }
            
            class B < A {
              class method() {
                super.method();
              }
            }
            
            B.method();
        """.trimIndent()
        mustFailResolving(source, "[line 9] Error at `super`: Can't use `super` in a static class method.")
    }
}
