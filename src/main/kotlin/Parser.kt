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
                varDeclaration()
            } else {
                statement()
            }
        } catch (error: ParseError) {
            synchronise()
            null
        }
    }

    private fun varDeclaration(): Statement {
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
        return Statement.VarDeclaration(name, initialiser)
    }

    private fun statement(): Statement {
        val matched = match(
            TokenType.Keyword.For,
            TokenType.Keyword.If,
            TokenType.LeftBrace,
            TokenType.Keyword.Print,
            TokenType.Keyword.While,
        )
        return when (matched?.first) {
            TokenType.Keyword.Print -> printStatement()
            TokenType.LeftBrace -> block()
            TokenType.Keyword.If -> ifStatement()
            TokenType.Keyword.While -> whileStatement()
            TokenType.Keyword.For -> forStatement(matched.second)
            else -> expressionStatement()
        }
    }

    private fun printStatement(): Statement.Print {
        val value = expression()
        consume(TokenType.Semicolon, "Expect ';' after value.")
        return Statement.Print(value)
    }

    private fun block(): Statement.Block {
        val statements = mutableListOf<Statement>()

        while (!check(TokenType.RightBrace) && moreTokens()) {
            declaration().let {
                if (it != null) statements.add(it)
            }
        }
        consume(TokenType.RightBrace, "Expect '}' after block.")

        return Statement.Block(statements.toList())
    }

    private fun ifStatement(): Statement.If {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RightParenthesis, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = match(TokenType.Keyword.Else)?.let { statement() }

        return Statement.If(condition, thenBranch, elseBranch)
    }

    private fun whileStatement(): Statement.While {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RightParenthesis, "Expect ')' after while condition.")
        val body = statement()

        return Statement.While(condition, body)
    }

    private fun forStatement(sourceLine: Int): Statement {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'for'.")
        val initialiser = when (match(TokenType.Semicolon, TokenType.Keyword.Var)?.first) {
            TokenType.Semicolon -> null
            TokenType.Keyword.Var -> varDeclaration()
            else -> expressionStatement()
        }

        val condition = if (!check(TokenType.Semicolon)) {
            expression()
        } else {
            null
        }
        consume(TokenType.Semicolon, "Expect ';' after loop condition.")

        val increment = if (!check(TokenType.RightParenthesis)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RightParenthesis, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Statement.Block(listOf(body, Statement.ExpressionStatement(increment)))
        }

        body = Statement.While(condition ?: Expression.Literal(TokenType.KeywordLiteral.True, sourceLine), body)

        if (initialiser != null) {
            body = Statement.Block(listOf(initialiser, body))
        }

        return body
    }

    private fun expressionStatement(): Statement.ExpressionStatement {
        val expr = expression()
        consume(TokenType.Semicolon, "Expect ';' after expression.")
        return Statement.ExpressionStatement(expr)
    }

    private fun expression(): Expression {
        return assignment()
    }

    private fun assignment(): Expression {
        val expression = or()

        val equals = match(TokenType.Equals)
        if (equals != null) {
            val value = assignment()

            if (expression is Expression.Variable) {
                return Expression.Assignment(expression.name, value)
            }

            errors.recordError("Invalid assignment target.", equals.second, " at ${equals.first}")
        }

        return expression
    }

    private fun or(): Expression {
        var expr = and()

        while (check(TokenType.Keyword.Or)) {
            val or = advance()!!
            val right = and()
            expr = Expression.Or(expr, right, or.line)
        }

        return expr
    }

    private fun and(): Expression {
        var expr = equality()

        while (check(TokenType.Keyword.And)) {
            val or = advance()!!
            val right = equality()
            expr = Expression.And(expr, right, or.line)
        }

        return expr
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
            return Expression.Variable(next)
        }

        parseError("Expected expression.")
    }

    // TODO could probably make the callers that want the line number use check & advance instead to simplify this (or return the actual matched token?)
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
