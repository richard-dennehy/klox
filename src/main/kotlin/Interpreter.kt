class Interpreter(private val io: IO) {
    private val builtins: MutableMap<String, LoxValue> = mutableMapOf(
        "clock" to LoxValue.Function("clock", 0, Scope(mutableMapOf())) { _, _, _ -> LoxValue.Number(io.currentTime()) }
    )

    private var scope = Scope(builtins)
    private val locals = mutableMapOf<Expression, VariableMetadata>()

    fun interpret(statements: List<Statement>): InterpreterResult {
        return try {
            InterpreterResult.Success(statements.map(::execute).lastOrNull())
        } catch (error: InterpreterResult.Error) {
            error
        }
    }

    fun resolve(expr: Expression, depth: Int, index: Int) {
        locals[expr] = VariableMetadata(depth, index)
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
                scope.define(statement.initialiser?.let { evaluate(it, statement.sourceLine) })
                ""
            }

            is Statement.Block -> {
                val previous = scope
                scope = Scope(scope.builtins, scope)
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
                scope.define(function)
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
                scope.getAt(expr.name, locals[expr])
            }
            is Expression.Assignment -> evaluate(expr.value, sourceLine).also { value ->
                scope.assignAt(expr.assignee, value, locals[expr])
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
    ): (Interpreter, Scope, List<LoxValue>) -> LoxValue = { interpreter, closure, arguments ->
        var returnValue: LoxValue = LoxValue.Nil

        val previous = interpreter.scope
        interpreter.scope = Scope(previous.builtins, closure)
        try {
            parameters.zip(arguments).forEach { (_, value) ->
                interpreter.scope.define(value)
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

class Scope(internal val builtins: MutableMap<String, LoxValue>, private val parent: Scope? = null) {
    private val values: MutableList<VariableState> = mutableListOf()

    internal fun getAt(name: Token, metadata: VariableMetadata?): LoxValue = if (metadata != null) {
        when (val value = ancestor(metadata.depth).values[metadata.index]) {
            is VariableState.Initialised -> value.value
            VariableState.Uninitialised -> throw uninitialised(name)
        }
    } else {
        when (val builtin = builtins[name.lexeme]) {
            null -> throw undefined(name)
            else -> builtin
        }
    }

    internal fun define(value: LoxValue?) {
        values.add(if (value != null) {
            VariableState.Initialised(value)
        } else {
            VariableState.Uninitialised
        })
    }

    internal fun assignAt(name: Token, value: LoxValue, metadata: VariableMetadata?) = if (metadata != null) {
        ancestor(metadata.depth).values[metadata.index] = VariableState.Initialised(value)
    } else {
        if (builtins[name.lexeme] != null) {
            throw InterpreterResult.Error(name.line, "Cannot assign to native function `${name.lexeme}`")
        } else {
            throw undefined(name)
        }
    }

    private fun ancestor(distance: Int): Scope {
        return (0 until distance).fold(this) { env, _ -> env.parent!! }
    }

    private fun undefined(token: Token): InterpreterResult.Error {
        return InterpreterResult.Error(token.line, "Undefined variable `${token.type.asString}`.")
    }

    private fun uninitialised(token: Token): InterpreterResult.Error {
        return InterpreterResult.Error(token.line, "Uninitialised variable `${token.type.asString}`")
    }
}

sealed interface VariableState {
    object Uninitialised : VariableState
    data class Initialised(val value: LoxValue) : VariableState
}

sealed interface InterpreterResult {
    data class Success(val data: String?) : InterpreterResult
    class Error(line: Int, msg: String) : RuntimeException(), InterpreterResult {
        override val message: String = "${msg}\n[line ${line}]"
    }
}

internal data class VariableMetadata(val depth: Int, val index: Int)

private object Break : RuntimeException()
private class Return(val value: LoxValue) : RuntimeException()
