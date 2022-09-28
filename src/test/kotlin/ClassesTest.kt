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

    @Test
    fun `this in class method`() {
        val source = """
class Cake {
  taste() {
    var adjective = "delicious";
    print "The " + this.flavor + " cake is " + adjective + "!";
  }
}

var cake = Cake();
cake.flavor = "German chocolate";
cake.taste();
        """.trimIndent()
        mustEvaluateTo(source, "nil")
        mustHavePrinted("The German chocolate cake is delicious!")
    }

    @Test
    fun `bound methods`() {
        val source = """
            class Person {
              sayName() {
                print this.name;
              }
            }

            var jane = Person();
            jane.name = "Jane";

            var method = jane.sayName;
            method();
        """.trimIndent()
        mustEvaluateTo(source, "nil")
        mustHavePrinted("Jane")
    }

    @Test
    fun `reassigning bound methods`() {
        val source = """
            class Person {
              sayName() {
                print this.name;
              }
            }

            var jane = Person();
            jane.name = "Jane";

            var bill = Person();
            bill.name = "Bill";

            bill.sayName = jane.sayName;
            bill.sayName();
        """.trimIndent()
        mustEvaluateTo(source, "nil")
        mustHavePrinted("Jane")
    }

    @Test
    fun `using this inside local function inside method`() {
        val source = """
            class Thing {
              getCallback() {
                fun localFunction() {
                  print this;
                }

                return localFunction;
              }
            }

            var callback = Thing().getCallback();
            callback();
        """.trimIndent()
        mustEvaluateTo(source, "nil")
        mustHavePrinted("Thing instance")
    }

    @Test
    fun `defining a custom constructor using init`() {
        val source = """
            class Cake {
              init(adjective, flavour) {
                this.adjective = adjective;
                this.flavour = flavour;              
              }
              
              taste() {
                print "The " + this.flavour + " cake is " + this.adjective + "!";
              }
            }

            var cake = Cake("delicious", "German chocolate");
            cake.taste();
        """.trimIndent()
        mustEvaluateTo(source, "nil")
        mustHavePrinted("The German chocolate cake is delicious!")
    }

    @Test
    fun `calling init method on class instance`() {
        val source = """
            class Foo {
              init() {
                print this;
              }
            }

            var foo = Foo();
            print foo.init();
        """.trimIndent()
        mustEvaluateTo(source, "")
        mustHavePrinted("Foo instance", "Foo instance", "Foo instance")
    }

    @Test
    fun `returning early from init method`() {
        val source = """
            class Stuff {
                init(earlyReturn) {
                    if (earlyReturn) return;
                    
                    print this.willFailTheTestIfReached;
                }
            }
            Stuff(true);
        """.trimIndent()
        mustEvaluateTo(source, "Stuff instance")
    }

    @Test
    fun `passing arguments to default class constructor`() {
        val source = """
            class Thing {}
            Thing(1, 2, 3, 4);
        """.trimIndent()
        mustFailExecution(source, "Expected 0 arguments but got 4.\n[line 2]")
    }

    @Test
    fun `passing too few arguments to class with init method`() {
        val source = """
            class Thing {
                init(a, b, c) {
                    print a + b + c;
                }
            }
            Thing(1, 2);
        """.trimIndent()
        mustFailExecution(source, "Expected 3 arguments but got 2.\n[line 6]")
    }

    @Test
    fun `passing too many arguments to class with init method`() {
        val source = """
            class Thing {
                init(a, b, c) {
                    print a + b + c;
                }
            }
            Thing(1, 2, 3, 4, 5);
        """.trimIndent()
        mustFailExecution(source, "Expected 3 arguments but got 5.\n[line 6]")
    }

    @Test
    fun `using this in a top level statement`() {
        val source = "print this;"
        mustFailResolving(source, "[line 1] Error at this: Can't use 'this' outside of a class.")
    }

    @Test
    fun `using this inside a top level function`() {
        val source = """
            fun notAMethod() {
              print this;
            }
        """.trimIndent()
        mustFailResolving(source, "[line 2] Error at this: Can't use 'this' outside of a class.")
    }

    @Test
    fun `returning a value from an initialiser`() {
        val source = """
            class Thing {
                init() {
                    return "oops";
                }
            }
            Thing();
        """.trimIndent()
        mustFailResolving(source, "[line 3] Error at return: Can't return a value from an initialiser.")
    }
}
