data class Token(val type: TokenType, val line: Int)

sealed interface TokenType {
    val asString: String

    object EOF : TokenType {
        override val asString: String = "EOF"
    }

    sealed interface Literal : TokenType
    data class Identifier(override val asString: String) : Literal

    data class LoxString(override val asString: String) : Literal

    data class LoxNumber(val asDouble: Double) : Literal {
        override val asString: String = asDouble.toString().removeSuffix(".0")
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

    enum class Keyword(override val asString: String): TokenType {
        And("and"),
        Break("break"),
        Class("class"),
        Else("else"),
        Fun("fun"),
        For("for"),
        If("if"),
        Or("or"),
        Print("print"),
        Return("return"),
        Super("super"),
        This("this"),
        Var("var"),
        While("while"),
    }

    enum class KeywordLiteral(override val asString: String): Literal {
        False("false"),
        Nil("nil"),
        True("true"),
    }
}
