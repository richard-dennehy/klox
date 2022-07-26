data class Token(val type: TokenType, val lexeme: String, val line: Int)

sealed interface TokenType {
    val asString: String

    object EOF : TokenType {
        override val asString: String = "EOF"
    }

    sealed interface Literal : TokenType
    data class Identifier(override val asString: String) : Literal

    data class LoxString(override val asString: String) : Literal

    data class LoxNumber(val value: Double) : Literal {
        override val asString: String = value.toString()
    }

    sealed interface BinaryOp : TokenType
    sealed interface UnaryOp : TokenType
    sealed class Symbol(override val asString: String) : TokenType

    object LeftParenthesis : Symbol("(")
    object RightParenthesis : Symbol(")")
    object LeftBrace : Symbol("{")
    object RightBrace : Symbol("}")
    object Comma : Symbol(",")
    object Dot : Symbol(".")
    object Minus : Symbol("-"), BinaryOp, UnaryOp
    object Plus : Symbol("+"), BinaryOp
    object Semicolon : Symbol(";")
    object Slash : Symbol("/"), BinaryOp
    object Asterisk : Symbol("*"), BinaryOp
    object Not : Symbol("!"), UnaryOp
    object NotEqual : Symbol("!="), BinaryOp
    object Equals : Symbol("=")
    object DoubleEquals : Symbol("=="), BinaryOp
    object GreaterThan : Symbol(">"), BinaryOp
    object GreaterThanOrEqual : Symbol(">="), BinaryOp
    object LessThan : Symbol("<"), BinaryOp
    object LessThanOrEqual : Symbol("<="), BinaryOp

    enum class Keyword(override val asString: String) : Literal {
        And("and"),
        Class("class"),
        Else("else"),
        False("false"),
        Fun("fun"),
        For("for"),
        If("if"),
        Nil("nil"),
        Or("or"),
        Print("print"),
        Return("return"),
        Super("super"),
        This("this"),
        True("true"),
        Var("var"),
        While("while"),
    }
}
