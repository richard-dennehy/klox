data class Token(val type: TokenType, val lexeme: String, val line: Int)

sealed interface TokenType
object EOF : TokenType

enum class Symbol : TokenType {
    LeftParenthesis,
    RightParenthesis,
    LeftBrace,
    RightBrace,
    Comma,
    Dot,
    Minus,
    Plus,
    Semicolon,
    Slash,
    Asterisk,
    Not,
    NotEqual,
    Equals,
    DoubleEquals,
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual,
}

enum class Keyword(val ident: String) : TokenType {
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

data class Identifier(val name: String) : TokenType
data class LoxString(val value: String) : TokenType
data class LoxNumber(val value: Double) : TokenType
