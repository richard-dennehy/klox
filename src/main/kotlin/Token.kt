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

enum class Keyword : TokenType {
    And,
    Class,
    Else,
    False,
    Fun,
    For,
    If,
    Nil,
    Or,
    Print,
    Return,
    Super,
    This,
    True,
    Var,
    While,
}

data class Identifier(val name: String) : TokenType
data class LoxString(val value: String) : TokenType
data class LoxNumber(val value: Double) : TokenType
