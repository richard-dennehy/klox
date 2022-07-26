sealed interface Expression {
    data class Binary(val left: Expression, val op: TokenType.BinaryOp, val right: Expression) : Expression
    data class Grouping(val expression: Expression) : Expression
    data class Unary(val op: TokenType.UnaryOp, val right: Expression) : Expression
    data class Literal(val lit: TokenType.Literal): Expression
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
