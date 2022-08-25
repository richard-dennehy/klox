import java.lang.RuntimeException

sealed interface Expression {
    val sourceLine: Int

    data class Binary(
        val left: Expression,
        val op: TokenType.BinaryOp,
        val right: Expression,
        override val sourceLine: Int
    ) : Expression

    data class Grouping(val expression: Expression, override val sourceLine: Int) : Expression
    data class Unary(val op: TokenType.UnaryOp, val right: Expression, override val sourceLine: Int) : Expression
    data class Literal(val lit: TokenType.Literal, override val sourceLine: Int) : Expression
}

fun astDebugString(expr: Expression): String {
    return when (expr) {
        is Expression.Binary -> {
            val op = expr.op.asString
            val left = astDebugString(expr.left)
            val right = astDebugString(expr.right)
            "($op $left $right)"
        }

        is Expression.Grouping -> "(group ${astDebugString(expr.expression)})"
        is Expression.Literal -> expr.lit.asString
        is Expression.Unary -> {
            val op = expr.op.asString
            "($op ${astDebugString(expr.right)})"
        }
    }
}

class Interpreter {
    fun interpret(expr: Expression): InterpreterResult {
        return try {
            InterpreterResult.Success(stringify(evaluate(expr)))
        } catch (error: InterpreterResult.Error) {
            error
        }
    }

    private fun stringify(value: Expression.Literal): String {
        return when (value.lit) {
            is TokenType.LoxNumber -> value.lit.asDouble.toString().removeSuffix(".0")
            is TokenType.LoxString -> "\"${value.lit.asString}\""
            else -> value.lit.asString
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun evaluate(expr: Expression): Expression.Literal {
        fun expectDouble(value: Expression.Literal, context: TokenType, sourceLine: Int): Double {
            return when (value.lit) {
                is TokenType.LoxNumber -> value.lit.asDouble
                else -> throw InterpreterResult.Error(Token(context, context.asString, sourceLine), "Operand must be a number")
            }
        }

        fun expectString(value: Expression.Literal, context: TokenType, sourceLine: Int): String {
            return when (value.lit) {
                is TokenType.LoxString -> value.lit.asString
                else -> throw InterpreterResult.Error(Token(context, context.asString, sourceLine), "Operand must be a string")
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

                        TokenType.LoxNumber(left / right)
                    }

                    TokenType.Minus -> {
                        val left = expectDouble(left, expr.op, expr.sourceLine)
                        val right = expectDouble(right, expr.op, expr.sourceLine)

                        TokenType.LoxNumber(left - right)
                    }

                    TokenType.Plus -> {
                        when (left.lit) {
                            is TokenType.LoxNumber -> TokenType.LoxNumber(left.lit.asDouble + expectDouble(right, expr.op, expr.sourceLine))
                            is TokenType.LoxString -> TokenType.LoxString(left.lit.asString + expectString(right, expr.op, expr.sourceLine))
                            else -> throw InterpreterResult.Error(
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

sealed interface InterpreterResult {
    data class Success(val data: String) : InterpreterResult
    data class Error(val token: Token, override val message: String?) : RuntimeException(), InterpreterResult
}
