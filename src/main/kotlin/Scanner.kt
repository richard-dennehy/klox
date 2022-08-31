// TODO this interface would make more sense as a function (maybe backed by a private class) since it can only be run once
class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0
    private var line = 1
    private val parseErrors = ParseErrors(mutableListOf())

    fun scanTokens(): ScanResult {
        var next = advance()
        while (next != null) {
            scanToken(next)
            start = current
            next = advance()
        }

        tokens.add(Token(TokenType.EOF, "", line))
        return ScanResult(tokens.toList(), parseErrors.asList())
    }

    private fun scanToken(char: Char) {
        val token: TokenType = when (char) {
            '(' -> TokenType.LeftParenthesis
            ')' -> TokenType.RightParenthesis
            '{' -> TokenType.LeftBrace
            '}' -> TokenType.RightBrace
            ',' -> TokenType.Comma
            '.' -> TokenType.Dot
            '-' -> TokenType.Minus
            '+' -> TokenType.Plus
            ';' -> TokenType.Semicolon
            '*' -> TokenType.Asterisk
            '!' -> if (match('=')) {
                TokenType.NotEqual
            } else {
                TokenType.Not
            }

            '=' -> if (match('=')) {
                TokenType.DoubleEquals
            } else {
                TokenType.Equals
            }

            '>' -> if (match('=')) {
                TokenType.GreaterThanOrEqual
            } else {
                TokenType.GreaterThan
            }

            '<' -> if (match('=')) {
                TokenType.LessThanOrEqual
            } else {
                TokenType.LessThan
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
                TokenType.Slash
            }

            ' ', '\r', '\t' -> return
            '\n' -> {
                line++
                return
            }

            else -> {
                parseErrors.recordError("Unexpected character.", line)
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
            parseErrors.recordError("Unterminated string.", line)
            return null
        }

        assert(advance() == '"')
        val value = source.substring(start + 1, current - 1)
        return TokenType.LoxString(value)
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
                parseErrors.recordError("Invalid numeric literal $raw", line)
                null
            }

            else -> TokenType.LoxNumber(value)
        }
    }

    private fun identifier(): TokenType {
        while (peek() in 'A'..'z' || peek() == '_') advance()

        val text = source.substring(start, current)
        return TokenType.Keyword.values().find { it.asString == text }
            ?: TokenType.KeywordLiteral.values().find { it.asString == text }
            ?: TokenType.Identifier(text)
    }

    private tailrec fun blockComment(nesting: Int = 0) {
        if (nesting < 0) return
        val startLine = line

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

        parseErrors.recordError("Unterminated block comment.", startLine)
    }
}

data class ScanResult(val tokens: List<Token>, val errors: List<String>)
