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
    fun `calling a method from another method`() {
        val source = """
            class Thing {
              a() {
                return this.b();
              }
              
              b() {
                return 1;
              }
            }
            
            Thing().a();
        """.trimIndent()
        mustEvaluateTo(source, "1")
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
    fun `assigning a field with the same name as instance method`() {
        val source = """
            class Stuff {
              method() {
                return 1;
              }
            }
            
            var s = Stuff();
            s.method = fun () { return 2; };
            s.method();
        """.trimIndent()
        mustEvaluateTo(source, "2")
    }

    @Test
    fun `static class methods`() {
        val source = """
            class Maths {
              class square(n) { return n * n; }
            }
            Maths.square(3);
        """.trimIndent()
        mustEvaluateTo(source, "9")
    }

    @Test
    fun `calling a static method from another static method`() {
        val source = """
            class Maths {
              class square(n) { return n * n; }
              class cube(n) { return Maths.square(n) * n; }
            }
            Maths.cube(3);
        """.trimIndent()
        mustEvaluateTo(source, "27")
    }

    @Test
    fun `calling a static method from an instance method`() {
        val source = """
            class Stuff {
              class a() { return 1; }
              a() { return Stuff.a(); }
            }
            Stuff().a();
        """.trimIndent()
        mustEvaluateTo(source, "1")
    }

    @Test
    fun `defining a class method and instance method with the same name`() {
        val source = """
            class Stuff {
              class a() { return "static call "; }
              a() { return "instance call"; }
            }
            Stuff.a() + Stuff().a();
        """.trimIndent()
        mustEvaluateTo(source, "\"static call instance call\"")
    }

    @Test
    fun `referring to a class within its definition`() {
        val source = """
            class Counted {
              init() {
                this.counter = 0;
              }
              
              clone() {
                var clone = Counted();
                clone.counter = clone.counter + this.counter + 1;
                return clone;
              }
            }
            
            Counted().clone().clone().clone().counter;
        """.trimIndent()
        mustEvaluateTo(source, "3")
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

    @Test
    fun `accessing a non-existent class property`() {
        val source = """
            class Thing {}
            Thing().doesntExist;
        """.trimIndent()
        mustFailExecution(source, "Undefined property `doesntExist`.\n[line 2]")
    }

    @Test
    fun `accessing a non-existent instance method`() {
        val source = """
            class Thing {}
            Thing().stillDoesntExist();
        """.trimIndent()
        mustFailExecution(source, "Undefined property `stillDoesntExist`.\n[line 2]")
    }

    @Test
    fun `accessing a non-existent class method`() {
        val source = """
            class Thing {}
            Thing.nope();
        """.trimIndent()
        mustFailExecution(source, "Undefined property `nope`.\n[line 2]")
    }

    @Test
    fun `accessing an instance method as a class method`() {
        val source = """
            class Thing {
              nope() { return "hi"; }
            }
            Thing.nope();
        """.trimIndent()
        mustFailExecution(source, "Undefined property `nope`.\n[line 4]")
    }

    @Test
    fun `calling a method from another method without using this`() {
        val source = """
            class Thing {
              a() {
                return b();
              }
              
              b() {
                return 1;
              }
            }
            
            Thing().a();
        """.trimIndent()
        mustFailExecution(source, "Undefined variable `b`.\n[line 3]")
    }

    @Test
    fun `using this in class method`() {
        val source = """
            class Stuff {
              class a() {
                return this.isNotAllowed;
              }
            }
            Stuff.a();
        """.trimIndent()
        mustFailResolving(source, "[line 3] Error at this: Can't use 'this' in static class method.")
    }

    @Test
    fun `using this in local function in class method`() {
        val source = """
            class Stuff {
              class a() {
                fun inner() {
                  return this.isStillNotAllowed;
                }
                return inner();
              }
            }
            Stuff.a();
        """.trimIndent()
        mustFailResolving(source, "[line 4] Error at this: Can't use 'this' in static class method.")
    }
}
