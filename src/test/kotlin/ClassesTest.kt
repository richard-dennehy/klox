import util.InterpreterTest
import kotlin.test.Test

class ClassesTest: InterpreterTest() {
    @Test
    fun `minimal class declaration`() {
        mustEvaluateTo("class Stuff {}", "Stuff")
    }

    @Test
    fun `constructing class instance`() {
        val source = """class Stuff {}
            |Stuff();
        """.trimMargin()
        mustEvaluateTo(source, "Stuff instance")
    }

    @Test
    fun `assigning and accessing class properties`() {
        val source = """class Stuff {}
            |var s = Stuff();
            |s.field = 123;
            |s.field;
        """.trimMargin()
        mustEvaluateTo(source, "123")
    }

    @Test
    fun `assigning function to class property`() {
        val source = """class Box {}

fun notMethod(argument) {
  return "called function with " + argument;
}

var box = Box();
box.function = notMethod;
box.function("argument");"""
        mustEvaluateTo(source, "\"called function with argument\"")
    }

    @Test
    fun `class methods`() {
        val source = """
class Bacon {
  eat() {
    return "Crunch crunch crunch!";
  }
}

Bacon().eat();"""
        mustEvaluateTo(source, "\"Crunch crunch crunch!\"")
    }
}
