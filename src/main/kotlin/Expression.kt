// can't use data classes because each instance needs to have a unique hash
sealed interface Expression {
    class Binary(
        val left: Expression,
        val op: TokenType.BinaryOp,
        val right: Expression
    ) : Expression

    class Grouping(val expression: Expression) : Expression
    class Unary(val op: TokenType.UnaryOp, val right: Expression) : Expression
    class Literal(val lit: TokenType.Literal) : Expression
    class Variable(val name: Token) : Expression
    class Assignment(val assignee: Token, val value: Expression) : Expression
    class And(val left: Expression, val right: Expression) : Expression
    class Or(val left: Expression, val right: Expression) : Expression
    class Call(val callee: Expression, val arguments: List<Expression>, val sourceLine: Int): Expression
    class Function(val parameters: List<Token>, val body: Statement.Block): Expression
    class Get(val obj: Expression, val name: Token): Expression
    class Set(val obj: Expression, val name: Token, val value: Expression): Expression
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
    data class Function(val name: Token, val parameters: List<Token>, val body: Block): Statement {
        override val sourceLine = name.line
    }
    data class Return(val value: Expression?, override val sourceLine: Int): Statement
    data class ClassDeclaration(val name: Token, val methods: List<Function>): Statement {
        override val sourceLine = name.line
    }
}
