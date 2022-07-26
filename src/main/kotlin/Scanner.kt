class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        var next = advance()
        while (next != null) {
            scanToken(next)
            start = current
            next = advance()
        }

        tokens.add(Token(EOF, "", line))
        return tokens.toList()
    }

    private fun scanToken(char: Char) {
        val token: TokenType = when (char) {
            '(' -> Symbol.LeftParenthesis
            ')' -> Symbol.RightParenthesis
            '[' -> Symbol.LeftBrace
            ']' -> Symbol.RightBrace
            ',' -> Symbol.Comma
            '.' -> Symbol.Dot
            '-' -> Symbol.Minus
            '+' -> Symbol.Plus
            ';' -> Symbol.Semicolon
            '*' -> Symbol.Asterisk
            '!' -> if (match('=')) {
                Symbol.NotEqual
            } else {
                Symbol.Not
            }
            '=' -> if (match('=')) {
                Symbol.DoubleEquals
            } else {
                Symbol.Equals
            }
            '>' -> if (match('=')) {
                Symbol.GreaterThanOrEqual
            } else {
                Symbol.GreaterThan
            }
            '<' -> if (match('=')) {
                Symbol.LessThanOrEqual
            } else {
                Symbol.LessThan
            }
            in 'A'..'z', '_' -> identifier()
            in '0'..'9' -> number() ?: return
            '"' -> string() ?: return
            '/' -> if (match('/')) {
                while (peek() != null && peek() != '\n') advance()
                return
            } else if (match('*')) {
                return blockComment()
            } else {
                Symbol.Slash
            }
            ' ', '\r', '\t' -> return
            '\n' -> {
                line++
                return
            }
            else -> {
                reportError("Unexpected character.", line)
                return
            }
        }

        tokens.add(Token(token, source.substring(start, current), line))
    }

    private fun advance(): Char? = source.getOrNull(current++)

    private fun peek(): Char? = source.getOrNull(current)

    private fun peekNext(): Char? = source.getOrNull(current + 1)

    private fun match(expected: Char): Boolean {
        if (peek() != expected) return false

        assert(advance() == expected)
        return true
    }

    private fun string(): TokenType? {
        while (peek() != null && peek() != '"') {
            if (peek() == '\n') line++
            advance()
        }

        if (peek() == null) {
            reportError("Unterminated string.", line)
            return null
        }

        assert(advance() == '"')
        val value = source.substring(start + 1, current - 1)
        return LoxString(value)
    }

    private fun number(): TokenType? {
        while (peek() in '0'..'9') advance()

        if (peek() == '.' && peekNext() in '0'..'9') {
            advance()

            while (peek() in '0'..'9') advance()
        }

        val raw = source.substring(start, current)
        return when (val value = raw.toDoubleOrNull()) {
            null -> {
                reportError("Invalid numeric literal $raw", line)
                null
            }
            else -> LoxNumber(value)
        }
    }

    private fun identifier(): TokenType {
        while (peek() in 'A'..'z' || peek() == '_') advance()

        val text = source.substring(start, current)
        return Keyword.values().find { it.ident == text } ?: Identifier(text)
    }

    private tailrec fun blockComment(nesting: Int = 0) {
        if (nesting < 0) return

        while (peek() != null) {
            when (advance()) {
                '\n' -> line++
                '*' -> if (match('/')) {
                    return blockComment(nesting = nesting - 1)
                }
                '/' -> if (match('*')) {
                    return blockComment(nesting = nesting + 1)
                }
            }
        }
    }
}
