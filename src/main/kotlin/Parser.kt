import java.lang.RuntimeException

fun parse(tokens: List<Token>): ParseResult = Parser(tokens).parse()

data class ParseResult(val statements: List<Statement>, val errors: List<String>)

private class Parser(private val tokens: List<Token>) {
    private val errors = ParseErrors(mutableListOf())
    private var current = 0

    fun parse(): ParseResult {
        errors.underlying.clear()
        val statements = mutableListOf<Statement>()

        while (moreTokens()) {
            val declaration = declaration(breakable = false)
            if (declaration != null) {
                statements.add(declaration)
            }
        }

        return ParseResult(statements, errors.asList())
    }

    private fun declaration(breakable: Boolean): Statement? {
        return try {
            val token = match(TokenType.Keyword.Fun, TokenType.Keyword.Var)
            when (token?.type) {
                TokenType.Keyword.Fun -> namedFunction("function")
                TokenType.Keyword.Var -> varDeclaration(token.line)
                else -> statement(breakable)
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

    private fun statement(breakable: Boolean): Statement {
        val matched = match(
            TokenType.Keyword.Break,
            TokenType.Keyword.For,
            TokenType.Keyword.If,
            TokenType.LeftBrace,
            TokenType.Keyword.Print,
            TokenType.Keyword.Return,
            TokenType.Keyword.While,
        )
        return when (matched?.type) {
            TokenType.Keyword.Print -> printStatement(matched.line)
            TokenType.LeftBrace -> block(matched.line, breakable)
            TokenType.Keyword.If -> ifStatement(matched.line, breakable)
            TokenType.Keyword.While -> whileStatement(matched.line)
            TokenType.Keyword.For -> forStatement(matched.line)
            TokenType.Keyword.Break -> if (breakable) {
                consume(TokenType.Semicolon, "Expect ';' after 'break'.")
                Statement.Break(matched.line)
            } else {
                parseError("'break' not inside a loop.")
            }
            TokenType.Keyword.Return -> {
                val value = if (!check(TokenType.Semicolon)) {
                    expression()
                } else {
                    null
                }

                consume(TokenType.Semicolon, "Expect ';' after return value.")
                Statement.Return(value, matched.line)
            }

            else -> expressionStatement()
        }
    }

    private fun printStatement(sourceLine: Int): Statement.Print {
        val value = expression()
        consume(TokenType.Semicolon, "Expect ';' after value.")
        return Statement.Print(value, sourceLine)
    }

    private fun block(sourceLine: Int, breakable: Boolean): Statement.Block {
        val statements = mutableListOf<Statement>()

        while (!check(TokenType.RightBrace) && moreTokens()) {
            declaration(breakable).let {
                if (it != null) statements.add(it)
            }
        }
        consume(TokenType.RightBrace, "Expect '}' after block.")

        return Statement.Block(statements, sourceLine)
    }

    private fun ifStatement(sourceLine: Int, breakable: Boolean): Statement.If {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RightParenthesis, "Expect ')' after if condition.")

        val thenBranch = statement(breakable)
        val elseBranch = match(TokenType.Keyword.Else)?.let { statement(breakable) }

        return Statement.If(condition, thenBranch, elseBranch, sourceLine)
    }

    private fun whileStatement(sourceLine: Int): Statement.While {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RightParenthesis, "Expect ')' after while condition.")
        val body = statement(breakable = true)

        return Statement.While(condition, body, sourceLine)
    }

    private fun forStatement(sourceLine: Int): Statement {
        consume(TokenType.LeftParenthesis, "Expect '(' after 'for'.")
        val matched = match(TokenType.Semicolon, TokenType.Keyword.Var)
        val initialiser = when (matched?.type) {
            TokenType.Semicolon -> null
            TokenType.Keyword.Var -> varDeclaration(matched.line)
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

        var body = statement(breakable = true)

        if (increment != null) {
            body = Statement.Block(listOf(body, Statement.ExpressionStatement(increment, sourceLine)), sourceLine)
        }

        body = Statement.While(condition ?: Expression.Literal(TokenType.KeywordLiteral.True), body, sourceLine)

        if (initialiser != null) {
            body = Statement.Block(listOf(initialiser, body), sourceLine)
        }

        return body
    }

    private fun expressionStatement(): Statement.ExpressionStatement {
        val sourceLine = peek()?.line ?: parseError("Expected expression.")
        val expr = expression()
        consume(TokenType.Semicolon, "Expect ';' after expression.")
        return Statement.ExpressionStatement(expr, sourceLine)
    }

    // TODO could probably simplify this to rewrite all expressions `fun name(args) {...}` to `var name = fun (args) {...}` but need to implement class methods first
    private fun namedFunction(kind: String): Statement.Function {
        val name = if (peek()?.type is TokenType.Identifier) {
            advance()!!
        } else {
            parseError("Expect $kind name.")
        }

        val (parameters, body) = function(name.line, kind)

        return Statement.Function(name, parameters, body)
    }

    private fun function(sourceLine: Int, kind: String): Pair<List<Token>, Statement.Block> {
        consume(TokenType.LeftParenthesis, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RightParenthesis)) {
            do {
                if (parameters.size >= 255) {
                    errors.recordError(peek() ?: tokens.last(), "Can't have more than 255 parameters.")
                }

                if (peek()?.type is TokenType.Identifier) {
                    parameters.add(advance()!!)
                } else {
                    parseError("Expect parameter name.")
                }
            } while (match(TokenType.Comma) != null)
        }

        consume(TokenType.RightParenthesis, "Expect ')' after parameters.")
        consume(TokenType.LeftBrace, "Expect '{' before $kind body.")
        val body = block(sourceLine, breakable = false)

        return parameters to body
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

            errors.recordError("Invalid assignment target.", equals.line, " at ${equals.type.asString}")
        }

        return expression
    }

    private fun or(): Expression {
        var expr = and()

        while (check(TokenType.Keyword.Or)) {
            advance()!!
            val right = and()
            expr = Expression.Or(expr, right)
        }

        return expr
    }

    private fun and(): Expression {
        var expr = equality()

        while (check(TokenType.Keyword.And)) {
            advance()!!
            val right = equality()
            expr = Expression.And(expr, right)
        }

        return expr
    }

    private fun equality(): Expression = binary({ comparison() }, TokenType.DoubleEquals, TokenType.NotEqual)

    private fun comparison(): Expression = binary(
        { term() }, TokenType.GreaterThan, TokenType.GreaterThanOrEqual, TokenType.LessThanOrEqual, TokenType.LessThan
    )

    private fun term(): Expression = binary({ factor() }, TokenType.Minus, TokenType.Plus)

    private fun factor(): Expression = binary({ unary() }, TokenType.Slash, TokenType.Asterisk)

    private fun unary(): Expression {
        val token = match(TokenType.Not, TokenType.Minus) ?: return call()
        return Expression.Unary(token.type as TokenType.UnaryOp, unary())
    }

    private fun binary(operand: () -> Expression, vararg operators: TokenType.BinaryOp): Expression {
        val nextMatch = { match(*operators) }
        var expr = operand()
        var operator = nextMatch()

        while (operator != null) {
            expr = Expression.Binary(expr, operator.type as TokenType.BinaryOp, operand())
            operator = nextMatch()
        }

        return expr
    }

    private fun call(): Expression {
        var expression = primary()

        // apparently there's good reason for this weirdness
        while (true) {
            if (match(TokenType.LeftParenthesis) != null) {
                expression = finishCall(expression)
            } else {
                break
            }
        }

        return expression
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments = mutableListOf<Expression>()
        if (!check(TokenType.RightParenthesis)) {
            do {
                if (arguments.size >= 255) {
                    errors.recordError(peek() ?: tokens.last(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.Comma) != null)
        }

        val closeParen = consume(TokenType.RightParenthesis, "Expect ')' after arguments.")

        return Expression.Call(callee, arguments, closeParen.line)
    }

    private fun primary(): Expression {
        val literal = match(TokenType.KeywordLiteral.True, TokenType.KeywordLiteral.False, TokenType.KeywordLiteral.Nil)
        if (literal != null) {
            return Expression.Literal(literal.type as TokenType.Literal)
        }

        val next = peek() ?: parseError("Unexpected EOF in expression.")
        if (next.type is TokenType.LoxString) {
            advance()
            return Expression.Literal(next.type)
        }

        if (next.type is TokenType.LoxNumber) {
            advance()
            return Expression.Literal(next.type)
        }

        if (next.type == TokenType.LeftParenthesis) {
            advance()
            val grouped = expression()
            consume(TokenType.RightParenthesis, "Expected ')' after grouped expression.")
            return Expression.Grouping(grouped)
        }

        if (next.type is TokenType.Identifier) {
            advance()
            return Expression.Variable(next)
        }

        if (next.type == TokenType.Keyword.Fun) {
            advance()
            val (parameters, body) = function(next.line, "function")
            return Expression.Function(parameters, body)
        }

        parseError("Expected expression.")
    }

    private fun <T : TokenType> match(vararg tokens: T): Token? {
        for (token in tokens) {
            if (check(token)) {
                return advance()!!
            }
        }

        return null
    }

    private fun consume(tokenType: TokenType, errorMessage: String): Token {
        when (val token = match(tokenType)) {
            null -> parseError(errorMessage)
            else -> return token
        }
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

internal class ParseErrors(internal var underlying: MutableList<String>) {
    internal fun recordError(message: String, line: Int, where: String = "") {
        underlying.add("[line $line] Error$where: $message")
    }

    internal fun recordError(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            recordError(message, token.line, " at end")
        } else {
            recordError(message, token.line, " at '${token.type.asString}'")
        }
    }

    fun asList(): List<String> {
        return underlying.toList()
    }
}

private class ParseError : RuntimeException("Parse error")
