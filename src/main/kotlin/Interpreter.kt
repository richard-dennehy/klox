class Interpreter {
    private var scope = Environment()

    fun interpret(statements: List<Statement>): InterpreterResult {
        return try {
            InterpreterResult.Success(statements.map(this::execute).lastOrNull())
        } catch (error: InterpreterResult.Error) {
            error
        }
    }

    private fun execute(statement: Statement): String {
        return when (statement) {
            is Statement.ExpressionStatement -> stringify(evaluate(statement.expression))
            is Statement.Print -> {
                println(stringify(evaluate(statement.expression)))
                ""
            }
            is Statement.VarDeclaration -> {
                val value = if (statement.initialiser != null) {
                    evaluate(statement.initialiser).lit
                } else {
                    TokenType.KeywordLiteral.Nil
                }

                scope[statement.name] = value
                ""
            }
            is Statement.Block -> {
                val previous = scope
                scope = Environment(scope)
                try {
                    statement.statements.map(this::execute).lastOrNull() ?: ""
                } finally {
                    scope = previous
                }
            }
        }
    }

    private fun stringify(value: Expression.Literal): String {
        return when (value.lit) {
            is TokenType.LoxString -> "\"${value.lit.asString}\""
            else -> value.lit.asString
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun evaluate(expr: Expression): Expression.Literal {
        fun expectDouble(value: Expression.Literal, context: TokenType, sourceLine: Int): Double {
            return when (value.lit) {
                is TokenType.LoxNumber -> value.lit.asDouble
                else -> throw InterpreterResult.Error(
                    Token(context, context.asString, sourceLine),
                    "Operand must be a number"
                )
            }
        }

        fun isTruthy(value: Expression.Literal): Boolean {
            return when (value.lit) {
                TokenType.KeywordLiteral.Nil, TokenType.KeywordLiteral.False -> false
                else -> true
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
            is Expression.Grouping -> evaluate(expr.expression).lit
            is Expression.Unary -> {
                val operand = evaluate(expr.right)
                when (expr.op) {
                    TokenType.Minus -> {
                        val loxNumber = expectDouble(operand, expr.op, expr.sourceLine)
                        TokenType.LoxNumber(-loxNumber)
                    }

                    TokenType.Not -> loxBoolean(!isTruthy(operand))
                }
            }
            is Expression.Variable -> scope[expr.name]
            is Expression.Assignment -> evaluate(expr.value).lit.also { scope.assign(expr.assignee, it) }

            is Expression.Binary -> {
                val left = evaluate(expr.left)
                val right = evaluate(expr.right)

                when (expr.op) {
                    TokenType.Asterisk -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        TokenType.LoxNumber(left * right)
                    }

                    TokenType.Slash -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        if (right != 0.0) {
                            TokenType.LoxNumber(left / right)
                        } else {
                            throw InterpreterResult.Error(
                                Token(expr.op, expr.op.asString, expr.sourceLine),
                                "Division by zero"
                            )
                        }
                    }

                    TokenType.Minus -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

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
                                    Token(expr.op, expr.op.asString, expr.sourceLine),
                                    "Operand must be a string or a number"
                                )
                            }
                        } else {
                            throw InterpreterResult.Error(
                                Token(expr.op, expr.op.asString, expr.sourceLine),
                                "Operand must be a string or a number"
                            )
                        }
                    }

                    TokenType.GreaterThan -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        loxBoolean(left > right)
                    }

                    TokenType.GreaterThanOrEqual -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        loxBoolean(left >= right)
                    }

                    TokenType.LessThan -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        loxBoolean(left < right)
                    }

                    TokenType.LessThanOrEqual -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        loxBoolean(left <= right)
                    }

                    TokenType.DoubleEquals -> loxBoolean(left.lit == right.lit)
                    TokenType.NotEqual -> loxBoolean(left.lit != right.lit)
                }
            }
        }

        return Expression.Literal(lit, expr.sourceLine)
    }
}

// TODO introduce e.g. LoxValue rather than using Literal everywhere
private class Environment(private val parent: Environment? = null) {
    private val values: MutableMap<String, TokenType.Literal> = mutableMapOf()

    operator fun get(name: Token): TokenType.Literal {
        val value = values[name.lexeme]
        if (value != null) {
            return value
        }

        if (parent != null) {
            return parent[name]
        }

        throw undefined(name)
    }

    operator fun set(name: String, value: TokenType.Literal) {
        values[name] = value
    }

    fun assign(name: Token, value: TokenType.Literal) {
        if (values[name.lexeme] != null) {
            values[name.lexeme] = value
        } else if (parent != null) {
            parent.assign(name, value)
        } else {
            throw undefined(name)
        }
    }

    private fun undefined(token: Token): InterpreterResult.Error {
        return InterpreterResult.Error(token, "Undefined variable ${token.lexeme}.")
    }
}

sealed interface InterpreterResult {
    data class Success(val data: String?) : InterpreterResult
    data class Error(val token: Token, override val message: String?) : RuntimeException(), InterpreterResult
}
