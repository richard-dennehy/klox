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

class Interpreter() {
    fun interpret(expr: Expression): String? {
        try {
            return stringify(evaluate(expr))
        } catch (error: LoxRuntimeError) {
            reportRuntimeError(error)
        }

        return null
    }

    private fun stringify(value: Any): String {
        return when (value) {
            is Double -> value.toString().removeSuffix(".0")
            is TokenType.LoxNumber -> value.asString.removeSuffix(".0")
            is TokenType -> value.asString
            else -> value.toString()
        }
    }

    // TODO return a Literal from this and extract the value in `stringify` as this is confusing
    private fun evaluate(expr: Expression): Any {
        fun expectDouble(value: Any, context: TokenType, sourceLine: Int): Double {
            return when (value) {
                is TokenType.LoxNumber -> value.asDouble
                is Double -> value
                else -> throw LoxRuntimeError(Token(context, context.asString, sourceLine), "Operand must be a number")
            }
        }

        fun expectString(value: Any, context: TokenType, sourceLine: Int): String {
            return when (value) {
                is TokenType.LoxString -> value.asString
                is String -> value
                else -> throw LoxRuntimeError(Token(context, context.asString, sourceLine), "Operand must be a string")
            }
        }

        fun isTruthy(value: Any): Boolean {
            return when (value) {
                TokenType.KeywordLiteral.Nil, TokenType.KeywordLiteral.False, false -> false
                else -> true
            }
        }

        return when (expr) {
            is Expression.Literal -> expr.lit
            is Expression.Grouping -> evaluate(expr.expression)
            is Expression.Unary -> {
                val operand = evaluate(expr.right)
                when (expr.op) {
                    TokenType.Minus -> -expectDouble(operand, expr.op, expr.sourceLine)
                    TokenType.Not -> !isTruthy(operand)
                }
            }
            is Expression.Binary -> {
                val left = evaluate(expr.left)
                val right = evaluate(expr.right)

                when (expr.op) {
                    TokenType.Asterisk -> expectDouble(left, expr.op, expr.sourceLine) * expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.Slash -> expectDouble(left, expr.op, expr.sourceLine) / expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.Minus -> expectDouble(left, expr.op, expr.sourceLine) - expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.Plus -> {
                        when (left) {
                            is TokenType.LoxNumber -> left.asDouble + expectDouble(right, expr.op, expr.sourceLine)
                            is TokenType.LoxString -> left.asString + expectString(right, expr.op, expr.sourceLine)
                            else -> throw LoxRuntimeError(
                                Token(expr.op, expr.op.asString, expr.sourceLine),
                                "Operand must be a string or a number"
                            )
                        }
                    }

                    TokenType.GreaterThan -> expectDouble(left, expr.op, expr.sourceLine) > expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.GreaterThanOrEqual -> expectDouble(left, expr.op, expr.sourceLine) >= expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.LessThan -> expectDouble(left, expr.op, expr.sourceLine) < expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.LessThanOrEqual -> expectDouble(left, expr.op, expr.sourceLine) <= expectDouble(
                        right,
                        expr.op,
                        expr.sourceLine
                    )

                    TokenType.DoubleEquals -> left == right
                    TokenType.NotEqual -> left != right
                }
            }
        }
    }
}

data class LoxRuntimeError(val token: Token, override val message: String?) : RuntimeException()
