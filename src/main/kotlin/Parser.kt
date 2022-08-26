import java.lang.RuntimeException

// TODO it only makes sense to call this once per list, so the interface should be a function, not a class
class Parser(private val tokens: List<Token>) {
    class ParseError : RuntimeException("Parse error")
    private val errors = ParseErrors(mutableListOf())
    private var current = 0

    fun parse(): ParseResult {
        errors.underlying.clear()
        val statements = mutableListOf<Statement>()

        while (moreTokens()) {
            val declaration = declaration()
            if (declaration != null) {
                statements.add(declaration)
            }
        }

        return ParseResult(statements, errors.asList())
    }

    private fun declaration(): Statement? {
        return try {
            val varToken = match(TokenType.Keyword.Var)
            if (varToken != null) {
                varDeclaration(varToken.second)
            } else {
                statement()
            }
        } catch (error: ParseError) {
            synchronise()
            null
        }
    }

    private fun varDeclaration(sourceLine: Int): Statement {
        val next = peek()
        val name = if (next?.type is TokenType.Identifier) {
            advance()
            next.type.asString
        } else {
            parseError("Expect variable name.")
        }

        val initialiser = if (match(TokenType.Equals) != null) {
            expression()
        } else {
            null
        }

        consume(TokenType.Semicolon, "Expect ';' after variable declaration")
        return Statement.VarDeclaration(name, initialiser, sourceLine)
    }

    private fun statement(): Statement {
        val printToken = match(TokenType.Keyword.Print)

        return if (printToken != null) {
            printStatement(printToken.second)
        } else {
            expressionStatement()
        }
    }

    private fun printStatement(sourceLine: Int): Statement.Print {
        val value = expression()
        consume(TokenType.Semicolon, "Expect ';' after value.")
        return Statement.Print(value, sourceLine)
    }

    private fun expressionStatement(): Statement.ExpressionStatement {
        val expr = expression()
        consume(TokenType.Semicolon, "Expect ';' after expression.")
        return Statement.ExpressionStatement(expr, expr.sourceLine)
    }

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
            TokenType.LessThan
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

        if (next.type is TokenType.Identifier) {
            advance()
            return Expression.Variable(next.type.asString, next.line)
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

    private fun moreTokens(): Boolean = peek() != null && peek()?.type != TokenType.EOF

    private fun parseError(errorMessage: String): Nothing {
        errors.recordError(peek() ?: tokens.last(), errorMessage)
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

data class ParseResult(val statements: List<Statement>, val errors: List<String>)

class ParseErrors(internal var underlying: MutableList<String>) {
    internal fun recordError(message: String, line: Int, where: String = "") {
        underlying.add("[line $line] Error$where: $message")
    }

    internal fun recordError(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            recordError(message, token.line, " at end")
        } else {
            recordError(message, token.line, " at '${token.lexeme}'")
        }
    }

    fun asList(): List<String> {
        return underlying.toList()
    }
}
