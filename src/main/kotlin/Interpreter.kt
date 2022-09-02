import java.lang.Exception

class Interpreter(private val io: IO) {
    private var scope = Environment()

    fun interpret(statements: List<Statement>): InterpreterResult {
        return try {
            InterpreterResult.Success(statements.map(::execute).lastOrNull())
        } catch (error: InterpreterResult.Error) {
            error
        }
    }

    private fun execute(statement: Statement): String {
        return when (statement) {
            is Statement.ExpressionStatement -> stringify(evaluate(statement.expression, statement.sourceLine))
            is Statement.Print -> {
                io.print(stringify(evaluate(statement.expression, statement.sourceLine)))
                ""
            }

            is Statement.VarDeclaration -> {
                scope[statement.name] = statement.initialiser?.let { evaluate(it, statement.sourceLine).lit }
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

            is Statement.If -> if (isTruthy(evaluate(statement.condition, statement.sourceLine))) {
                execute(statement.thenBranch)
            } else {
                statement.elseBranch?.let(::execute) ?: ""
            }

            is Statement.While -> {
                var result = ""
                try {
                    while (isTruthy(evaluate(statement.condition, statement.sourceLine))) {
                        result = execute(statement.body)
                    }
                } catch (_: Break) {}
                result
            }
            is Statement.Break -> throw Break
        }
    }

    private fun stringify(value: Expression.Literal): String {
        return when (value.lit) {
            is TokenType.LoxString -> "\"${value.lit.asString}\""
            else -> value.lit.asString
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun evaluate(expr: Expression, sourceLine: Int): Expression.Literal {
        fun expectDouble(value: Expression.Literal, sourceLine: Int): Double {
            return when (value.lit) {
                is TokenType.LoxNumber -> value.lit.asDouble
                else -> throw InterpreterResult.Error(
                    sourceLine,
                    "Operand must be a number"
                )
            }
        }

        fun loxBoolean(jBool: Boolean): TokenType.KeywordLiteral {
            return if (jBool) {
                TokenType.KeywordLiteral.True
            } else {
                TokenType.KeywordLiteral.False
            }
        }

        val lit = when (expr) {
            is Expression.Literal -> expr.lit
            is Expression.Grouping -> evaluate(expr.expression, sourceLine).lit
            is Expression.Unary -> {
                val operand = evaluate(expr.right, sourceLine)
                when (expr.op) {
                    TokenType.Minus -> {
                        val loxNumber = expectDouble(operand, sourceLine)
                        TokenType.LoxNumber(-loxNumber)
                    }

                    TokenType.Not -> loxBoolean(!isTruthy(operand))
                }
            }

            is Expression.Variable -> scope[expr.name]
            is Expression.Assignment -> evaluate(expr.value, sourceLine).lit.also { scope.assign(expr.assignee, it) }
            is Expression.Or -> {
                val left = evaluate(expr.left, sourceLine)
                if (isTruthy(left)) {
                    left.lit
                } else {
                    evaluate(expr.right, sourceLine).lit
                }
            }
            is Expression.And -> {
                val left = evaluate(expr.left, sourceLine)
                if (!isTruthy(left)) {
                    left.lit
                } else {
                    evaluate(expr.right, sourceLine).lit
                }
            }

            is Expression.Binary -> {
                val left = evaluate(expr.left, sourceLine)
                val right = evaluate(expr.right, sourceLine)

                when (expr.op) {
                    TokenType.Asterisk -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        TokenType.LoxNumber(left * right)
                    }

                    TokenType.Slash -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        if (right != 0.0) {
                            TokenType.LoxNumber(left / right)
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

                        TokenType.LoxNumber(left - right)
                    }

                    TokenType.Plus -> {
                        if (left.lit is TokenType.LoxString || right.lit is TokenType.LoxString) {
                            TokenType.LoxString(left.lit.asString + right.lit.asString)
                        } else if (left.lit is TokenType.LoxNumber) {
                            if (right.lit is TokenType.LoxNumber) {
                                TokenType.LoxNumber(left.lit.asDouble + right.lit.asDouble)
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

                        loxBoolean(left > right)
                    }

                    TokenType.GreaterThanOrEqual -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        loxBoolean(left >= right)
                    }

                    TokenType.LessThan -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        loxBoolean(left < right)
                    }

                    TokenType.LessThanOrEqual -> {
                        val left = expectDouble(left, sourceLine)
                        val right = expectDouble(right, sourceLine)

                        loxBoolean(left <= right)
                    }

                    TokenType.DoubleEquals -> loxBoolean(left.lit == right.lit)
                    TokenType.NotEqual -> loxBoolean(left.lit != right.lit)
                }
            }
        }

        return Expression.Literal(lit)
    }

    private fun isTruthy(value: Expression.Literal): Boolean {
        return when (value.lit) {
            TokenType.KeywordLiteral.Nil, TokenType.KeywordLiteral.False -> false
            else -> true
        }
    }
}

// TODO introduce e.g. LoxValue rather than using Literal everywhere
private class Environment(private val parent: Environment? = null) {
    private val values: MutableMap<String, LoxVariable> = mutableMapOf()

    operator fun get(name: Token): TokenType.Literal =
        when (val maybeInitialised = values[name.type.asString]) {
            is LoxVariable.Initialised -> maybeInitialised.value
            is LoxVariable.Uninitialised -> throw uninitialised(name)
            null -> if (parent != null) {
                parent[name]
            } else {
                throw undefined(name)
            }
        }

    operator fun set(name: String, value: TokenType.Literal?) {
        values[name] = if (value != null) {
            LoxVariable.Initialised(value)
        } else {
            LoxVariable.Uninitialised
        }
    }

    fun assign(name: Token, value: TokenType.Literal) {
        if (values[name.type.asString] != null) {
            values[name.type.asString] = LoxVariable.Initialised(value)
        } else if (parent != null) {
            parent.assign(name, value)
        } else {
            throw undefined(name)
        }
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
    data class Initialised(val value: TokenType.Literal) : LoxVariable
}

sealed interface InterpreterResult {
    data class Success(val data: String?) : InterpreterResult
    class Error(line: Int, msg: String) : RuntimeException(), InterpreterResult {
        override val message: String = "${msg}\n[line ${line}]"
    }
}

private object Break: Exception()
