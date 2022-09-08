class Interpreter(private val io: IO) {
    private val globals = Environment().also {
        it["clock"] = LoxValue.Function("clock", 0, Environment()) { _, _, _ -> LoxValue.Number(io.currentTime()) }
    }

    private var scope = globals
    private val locals = mutableMapOf<Expression, Int>()

    fun interpret(statements: List<Statement>): InterpreterResult {
        return try {
            InterpreterResult.Success(statements.map(::execute).lastOrNull())
        } catch (error: InterpreterResult.Error) {
            error
        }
    }

    fun resolve(expr: Expression, depth: Int) {
        locals[expr] = depth
    }

    private fun execute(statement: Statement): String {
        return when (statement) {
            is Statement.ExpressionStatement -> evaluate(statement.expression, statement.sourceLine).asString
            is Statement.Print -> {
                io.print(
                    when (val result = evaluate(statement.expression, statement.sourceLine)) {
                        is LoxValue.String -> result.value
                        else -> result.asString
                    }
                )
                ""
            }

            is Statement.VarDeclaration -> {
                scope[statement.name] = statement.initialiser?.let { evaluate(it, statement.sourceLine) }
                ""
            }

            is Statement.Block -> {
                val previous = scope
                scope = Environment(scope)
                try {
                    statement.statements.map(::execute).lastOrNull() ?: ""
                } finally {
                    scope = previous
                }
            }

            is Statement.If -> if (evaluate(statement.condition, statement.sourceLine).isTruthy) {
                execute(statement.thenBranch)
            } else {
                statement.elseBranch?.let(::execute) ?: ""
            }

            is Statement.While -> {
                try {
                    while (evaluate(statement.condition, statement.sourceLine).isTruthy) {
                        execute(statement.body)
                    }
                } catch (_: Break) {
                }
                ""
            }

            is Statement.Function -> {
                val function = LoxValue.Function(
                    statement.name.type.asString, statement.parameters.size, scope,
                    buildFunctionImpl(statement.parameters, statement.body)
                )
                scope[statement.name.type.asString] = function
                ""
            }

            is Statement.Return -> throw Return(statement.value?.let { evaluate(it, statement.sourceLine) }
                ?: LoxValue.Nil)

            is Statement.Break -> throw Break
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun evaluate(expr: Expression, sourceLine: Int): LoxValue {
        fun expectDouble(value: LoxValue, sourceLine: Int): Double {
            return when (value) {
                is LoxValue.Number -> value.value
                else -> throw InterpreterResult.Error(
                    sourceLine,
                    "Operand must be a number"
                )
            }
        }

        return when (expr) {
            is Expression.Literal -> LoxValue.from(expr.lit)
            is Expression.Grouping -> evaluate(expr.expression, sourceLine)
            is Expression.Unary -> {
                val operand = evaluate(expr.right, sourceLine)
                when (expr.op) {
                    TokenType.Minus -> {
                        val loxNumber = expectDouble(operand, sourceLine)
                        LoxValue.Number(-loxNumber)
                    }

                    TokenType.Not -> LoxValue.from(!operand.isTruthy)
                }
            }

            is Expression.Variable -> {
                locals[expr]?.let { scope.getAt(it, expr.name) } ?: globals[expr.name]
            }
            is Expression.Assignment -> evaluate(expr.value, sourceLine).also { value ->
                when (val distance = locals[expr]) {
                    null -> globals.assign(expr.assignee, value)
                    else -> scope.assignAt(distance, expr.assignee, value)
                }
            }
            is Expression.Or -> {
                val left = evaluate(expr.left, sourceLine)
                if (left.isTruthy) {
                    left
                } else {
                    evaluate(expr.right, sourceLine)
                }
            }

            is Expression.And -> {
                val left = evaluate(expr.left, sourceLine)
                if (!left.isTruthy) {
                    left
                } else {
                    evaluate(expr.right, sourceLine)
                }
            }

            is Expression.Call -> {
                val callee = evaluate(expr.callee, sourceLine)

                if (callee !is LoxValue.Function) {
                    throw InterpreterResult.Error(expr.sourceLine, "Can only call functions and classes.")
                }

                val args = expr.arguments.map { evaluate(it, sourceLine) }
                if (args.size != callee.arity) {
                    throw InterpreterResult.Error(
                        expr.sourceLine,
                        "Expected ${callee.arity} arguments but got ${args.size}."
                    )
                }
                callee.call(this, callee.closure, args)
            }

            is Expression.Function -> {
                LoxValue.Function("[anon]", expr.parameters.size, scope, buildFunctionImpl(expr.parameters, expr.body))
            }

            is Expression.Binary -> {
                val left = evaluate(expr.left, sourceLine)
                val right = evaluate(expr.right, sourceLine)

                when (expr.op) {
                    TokenType.Asterisk -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.Number(left * right)
                    }

                    TokenType.Slash -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        if (right != 0.0) {
                            LoxValue.Number(left / right)
                        } else {
                            throw InterpreterResult.Error(
                                sourceLine,
                                "Division by zero"
                            )
                        }
                    }

                    TokenType.Minus -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.Number(left - right)
                    }

                    TokenType.Plus -> {
                        if (left is LoxValue.String && right is LoxValue.String) {
                            LoxValue.String(left.value + right.value)
                        } else if (left is LoxValue.String) {
                            LoxValue.String(left.value + right.asString)
                        } else if (right is LoxValue.String) {
                            LoxValue.String(left.asString + right.value)
                        } else if (left is LoxValue.Number) {
                            if (right is LoxValue.Number) {
                                LoxValue.Number(left.value + right.value)
                            } else {
                                throw InterpreterResult.Error(
                                    sourceLine,
                                    "Operand must be a string or a number"
                                )
                            }
                        } else {
                            throw InterpreterResult.Error(
                                sourceLine,
                                "Operand must be a string or a number"
                            )
                        }
                    }

                    TokenType.GreaterThan -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.from(left > right)
                    }

                    TokenType.GreaterThanOrEqual -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.from(left >= right)
                    }

                    TokenType.LessThan -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.from(left < right)
                    }

                    TokenType.LessThanOrEqual -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        LoxValue.from(left <= right)
                    }

                    TokenType.DoubleEquals -> LoxValue.from(left == right)
                    TokenType.NotEqual -> LoxValue.from(left != right)
                }
            }
        }
    }

    private fun buildFunctionImpl(
        parameters: List<Token>,
        body: Statement.Block
    ): (Interpreter, Environment, List<LoxValue>) -> LoxValue = { interpreter, closure, arguments ->
        var returnValue: LoxValue = LoxValue.Nil

        val previous = interpreter.scope
        interpreter.scope = Environment(closure)
        try {
            parameters.zip(arguments).forEach { (param, value) ->
                interpreter.scope[param.type.asString] = value
            }
            interpreter.execute(body)
        } catch (r: Return) {
            returnValue = r.value
        } finally {
            interpreter.scope = previous
        }

        returnValue
    }
}

class Environment(private val parent: Environment? = null) {
    private val values: MutableMap<String, LoxVariable> = mutableMapOf()

    operator fun get(name: Token): LoxValue =
        when (val maybeInitialised = values[name.type.asString]) {
            is LoxVariable.Initialised -> maybeInitialised.value
            is LoxVariable.Uninitialised -> throw uninitialised(name)
            null -> if (parent != null) {
                parent[name]
            } else {
                throw undefined(name)
            }
        }

    fun getAt(distance: Int, name: Token): LoxValue = ancestor(distance)[name]

    operator fun set(name: String, value: LoxValue?) {
        values[name] = if (value != null) {
            LoxVariable.Initialised(value)
        } else {
            LoxVariable.Uninitialised
        }
    }

    fun assign(name: Token, value: LoxValue) {
        if (values[name.type.asString] != null) {
            values[name.type.asString] = LoxVariable.Initialised(value)
        } else if (parent != null) {
            parent.assign(name, value)
        } else {
            throw undefined(name)
        }
    }

    fun assignAt(distance: Int, name: Token, value: LoxValue) = ancestor(distance).assign(name, value)

    private fun ancestor(distance: Int): Environment {
        return (0 until distance).fold(this) { env, _ -> env.parent!! }
    }

    private fun undefined(token: Token): InterpreterResult.Error {
        return InterpreterResult.Error(token.line, "Undefined variable `${token.type.asString}`.")
    }

    private fun uninitialised(token: Token): InterpreterResult.Error {
        return InterpreterResult.Error(token.line, "Uninitialised variable `${token.type.asString}`")
    }
}

sealed interface LoxVariable {
    object Uninitialised : LoxVariable
    data class Initialised(val value: LoxValue) : LoxVariable
}

sealed interface InterpreterResult {
    data class Success(val data: String?) : InterpreterResult
    class Error(line: Int, msg: String) : RuntimeException(), InterpreterResult {
        override val message: String = "${msg}\n[line ${line}]"
    }
}

private object Break : RuntimeException()
private class Return(val value: LoxValue) : RuntimeException()
