import java.util.Stack

fun resolve(interpreter: Interpreter, statements: List<Statement>): ResolveErrors =
    Resolver(interpreter).resolve(statements)

data class ResolveErrors(val errors: List<String>)

class Resolver(private val interpreter: Interpreter) {
    private val errors = mutableListOf<String>()
    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.None

    fun resolve(statements: List<Statement>): ResolveErrors {
        statements.forEach(::resolve)
        return ResolveErrors(errors)
    }

    private fun resolve(statement: Statement) {
        when (statement) {
            is Statement.Block -> {
                beginScope()
                resolve(statement.statements)
                endScope()
            }

            is Statement.Break -> {}
            is Statement.ExpressionStatement -> resolve(statement.expression)
            is Statement.Function -> {
                declare(statement.name.type.asString, statement.sourceLine)
                define(statement.name.type.asString)

                resolveFunction(statement.parameters, statement.body)
            }

            is Statement.If -> {
                resolve(statement.condition)
                resolve(statement.thenBranch)
                statement.elseBranch?.let(::resolve)
            }

            is Statement.Print -> resolve(statement.expression)
            is Statement.Return -> {
                if (currentFunction == FunctionType.None) {
                    recordError(statement.sourceLine, "return", "Can't return from top-level code.")
                }
                statement.value?.let(::resolve)
            }
            is Statement.VarDeclaration -> {
                declare(statement.name, statement.sourceLine)
                statement.initialiser?.let(::resolve)
                define(statement.name)
            }

            is Statement.While -> {
                resolve(statement.condition)
                resolve(statement.body)
            }
        }
    }

    private fun resolve(expr: Expression) {
        when (expr) {
            is Expression.And -> {
                resolve(expr.left)
                resolve(expr.right)
            }

            is Expression.Assignment -> {
                resolve(expr.value)
                resolveLocal(expr, expr.assignee)
            }

            is Expression.Binary -> {
                resolve(expr.left)
                resolve(expr.right)
            }

            is Expression.Call -> {
                resolve(expr.callee)
                expr.arguments.forEach(::resolve)
            }

            is Expression.Function -> resolveFunction(expr.parameters, expr.body)
            is Expression.Grouping -> resolve(expr.expression)
            is Expression.Literal -> {}
            is Expression.Or -> {
                resolve(expr.left)
                resolve(expr.right)
            }

            is Expression.Unary -> resolve(expr.right)
            is Expression.Variable -> {
                if (scopes.isNotEmpty() && scopes.peek()[expr.name.type.asString] == false) {
                    recordError(expr.name.line, expr.name.type.asString, "Can't read local variable in its own initialiser.")
                }

                resolveLocal(expr, expr.name)
            }
        }
    }

    private fun resolveFunction(parameters: List<Token>, body: Statement.Block) {
        val enclosing = currentFunction
        currentFunction = FunctionType.Function

        beginScope()
        parameters.forEach {
            declare(it.type.asString, it.line)
            define(it.type.asString)
        }
        resolve(body.statements)
        endScope()

        currentFunction = enclosing
    }

    private fun resolveLocal(expr: Expression, name: Token) {
        scopes.withIndex().reversed().forEach { (index, scope) ->
            if (scope.containsKey(name.type.asString)) {
                interpreter.resolve(expr, scopes.lastIndex - index)
                return
            }
        }
    }

    private fun declare(name: String, line: Int) {
        if (scopes.isNotEmpty()) {
            val scope = scopes.peek()
            if (scope.containsKey(name)) {
                recordError(line, name, "Already a variable with this name in this scope.")
            }
            scope[name] = false
        }
    }

    private fun define(name: String) {
        if (scopes.isNotEmpty()) {
            scopes.peek()[name] = true
        }
    }

    private fun beginScope() = scopes.push(mutableMapOf())
    private fun endScope() = scopes.pop()

    private fun recordError(line: Int, lexeme: String, message: String) {
        errors.add("[line $line] Error $lexeme: $message")
    }
}

enum class FunctionType {
    Function,
    None,
}
