import java.lang.RuntimeException

class Parser(private val tokens: List<Token>) {
    class ParseError : RuntimeException("Parse error")

    fun parse(): Expression? {
        return try {
            expression()
        } catch (error: ParseError) {
            null
        }
    }

    private var current = 0

    private fun expression(): Expression {
        return equality()
    }

    private fun equality(): Expression = binary({ comparison() }, TokenType.DoubleEquals, TokenType.NotEqual)

    private fun comparison(): Expression =
        binary(
            { term() },
            TokenType.GreaterThan,
            TokenType.GreaterThanOrEqual,
            TokenType.LessThanOrEqual,
            TokenType.LessThanOrEqual
        )

    private fun term(): Expression = binary({ factor() }, TokenType.Minus, TokenType.Plus)

    private fun factor(): Expression = binary({ unary() }, TokenType.Slash, TokenType.Asterisk)

    private fun unary(): Expression {
        val token = match(TokenType.Not, TokenType.Minus) ?: return primary()
        return Expression.Unary(token.first, unary(), token.second)
    }

    private fun binary(operand: () -> Expression, vararg operators: TokenType.BinaryOp): Expression {
        val nextMatch = { match(*operators) }
        var expr = operand()
        var operator = nextMatch()

        while (operator != null) {
            expr = Expression.Binary(expr, operator.first, operand(), operator.second)
            operator = nextMatch()
        }

        return expr
    }

    private fun primary(): Expression {
        val literal = match(TokenType.KeywordLiteral.True, TokenType.KeywordLiteral.False, TokenType.KeywordLiteral.Nil)
        if (literal != null) {
            return Expression.Literal(literal.first, literal.second)
        }

        val next = peek() ?: parseError("Unexpected EOF in expression.")
        if (next.type is TokenType.LoxString) {
            advance()
            return Expression.Literal(next.type, next.line)
        }

        if (next.type is TokenType.LoxNumber) {
            advance()
            return Expression.Literal(next.type, next.line)
        }

        if (next.type == TokenType.LeftParenthesis) {
            advance()
            val grouped = expression()
            consume(TokenType.RightParenthesis, "Expected ')' after grouped expression.")
            return Expression.Grouping(grouped, next.line)
        }

        parseError("Expected expression.")
    }

    private fun <T : TokenType> match(vararg tokens: T): Pair<T, Int>? {
        for (token in tokens) {
            if (check(token)) {
                val matched = advance()!!
                return token to matched.line
            }
        }

        return null
    }

    private fun consume(tokenType: TokenType, errorMessage: String): Token? {
        if (check(tokenType)) {
            return advance()
        }

        parseError(errorMessage)
    }

    private fun check(token: TokenType): Boolean {
        return peek()?.type == token
    }

    private fun advance(): Token? {
        return if (current >= tokens.size) {
            null
        } else {
            tokens[current++]
        }
    }

    private fun peek(): Token? = tokens.getOrNull(current)

    private fun parseError(errorMessage: String): Nothing {
        reportParseError(peek() ?: tokens.last(), errorMessage)
        throw ParseError()
    }

    private fun synchronise() {
        var token = advance()

        while (token != null) {
            if (token.type == TokenType.Semicolon) return

            if (peek()?.type in arrayOf(
                    TokenType.Keyword.Class,
                    TokenType.Keyword.Fun,
                    TokenType.Keyword.Var,
                    TokenType.Keyword.For,
                    TokenType.Keyword.If,
                    TokenType.Keyword.While,
                    TokenType.Keyword.Print,
                    TokenType.Keyword.Return
                )
            ) {
                return
            } else {
                token = advance()
            }
        }
    }
}
