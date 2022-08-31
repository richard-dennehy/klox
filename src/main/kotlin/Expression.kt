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
    data class Variable(val name: Token): Expression {
        override val sourceLine: Int = name.line
    }
    data class Assignment(val assignee: Token, val value: Expression): Expression {
        override val sourceLine: Int = assignee.line
    }
}

sealed interface Statement {
    val sourceLine: Int

    data class ExpressionStatement(val expression: Expression, override val sourceLine: Int): Statement
    data class Print(val expression: Expression, override val sourceLine: Int): Statement
    data class VarDeclaration(val name: String, val initialiser: Expression?, override val sourceLine: Int): Statement
    data class Block(val statements: List<Statement>, override val sourceLine: Int): Statement
}
