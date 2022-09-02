sealed interface Expression {
    data class Binary(
        val left: Expression,
        val op: TokenType.BinaryOp,
        val right: Expression
    ) : Expression

    data class Grouping(val expression: Expression) : Expression
    data class Unary(val op: TokenType.UnaryOp, val right: Expression) : Expression
    data class Literal(val lit: TokenType.Literal) : Expression
    data class Variable(val name: Token) : Expression
    data class Assignment(val assignee: Token, val value: Expression) : Expression
    data class And(val left: Expression, val right: Expression) : Expression
    data class Or(val left: Expression, val right: Expression) : Expression
}

sealed interface Statement {
    val sourceLine: Int

    data class ExpressionStatement(val expression: Expression, override val sourceLine: Int) : Statement
    data class Print(val expression: Expression, override val sourceLine: Int) : Statement
    data class VarDeclaration(val name: String, val initialiser: Expression?, override val sourceLine: Int) : Statement
    data class Block(val statements: List<Statement>, override val sourceLine: Int) : Statement
    data class If(
        val condition: Expression,
        val thenBranch: Statement,
        val elseBranch: Statement?,
        override val sourceLine: Int
    ) : Statement

    data class While(val condition: Expression, val body: Statement, override val sourceLine: Int) : Statement
    data class Break(override val sourceLine: Int) : Statement
}
