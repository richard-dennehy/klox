class Parser(private val tokens: List<Token>) {
    private var current = 0

    private fun expression(): Expression {
        return equality()
    }

    private fun equality(): Expression {
        var expr = comparison()

        while (match(TokenType.DoubleEquals, TokenType.NotEqual)) {
            val operator = previous()
            if (operator.type is TokenType.BinaryOp) {
                val right = comparison()
                expr = Expression.Binary(expr, operator.type, right)
            } else {
                TODO("this shouldn't actually happen - try to fix the API")
            }
        }

        return expr
    }

    private fun comparison(): Expression {
        var expr = term()

        while (match(
                TokenType.GreaterThan,
                TokenType.GreaterThanOrEqual,
                TokenType.LessThan,
                TokenType.LessThanOrEqual
            )
        ) {
            val operator = previous()
            if (operator.type is TokenType.BinaryOp) {
                val right = term()
                expr = Expression.Binary(expr, operator.type, right)
            } else {
                TODO("this shouldn't actually happen - try to fix the API")
            }
        }

        return expr
    }

    private fun term(): Expression {
        var expr = factor()

        while (match(TokenType.Minus, TokenType.Plus)) {
            val operator = previous()
            if (operator.type is TokenType.BinaryOp) {
                val right = factor()
                expr = Expression.Binary(expr, operator.type, right)
            } else {
                TODO("this shouldn't actually happen - try to fix the API")
            }
        }

        return expr
    }

    private fun factor(): Expression {
        var expr = unary()

        while (match(TokenType.Slash, TokenType.Asterisk)) {
            val operator = previous()
            if (operator.type is TokenType.BinaryOp) {
                val right = unary()
                expr = Expression.Binary(expr, operator.type, right)
            } else {
                TODO("this shouldn't actually happen - try to fix the API")
            }
        }

        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.Not, TokenType.Minus)) {
            val operator = previous()
            if (operator.type is TokenType.UnaryOp) {
                val right = unary()
                return Expression.Unary(operator.type, right)
            }
        }

        return primary()
    }

    private fun primary(): Expression {
        if (match(TokenType.Keyword.False)) return Expression.Literal(TokenType.Keyword.False)
        if (match(TokenType.Keyword.True)) return Expression.Literal(TokenType.Keyword.True)
        if (match(TokenType.Keyword.Nil)) return Expression.Literal(TokenType.Keyword.Nil)

        // TODO this isn't right - change match to optionally return what it matched
        val next = peek() ?: TODO("error handling")
        if (next.type is TokenType.Literal) {
            return Expression.Literal(next.type)
        }

        TODO()
    }

    private fun match(vararg tokens: TokenType): Boolean {
        for (token in tokens) {
            if (check(token)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun check(token: TokenType): Boolean {
        return peek()?.type == token
    }

    private fun advance(): Token? {
        return if (current >= tokens.size) {
            null
        } else {
            advance()
            previous()
        }
    }

    private fun peek(): Token? = tokens.getOrNull(current)
    private fun previous(): Token = tokens[current - 1]
}
