import java.util.Stack

fun resolve(interpreter: Interpreter, statements: List<Statement>): ResolveErrors =
    Resolver(interpreter).resolve(statements)

data class ResolveErrors(val errors: List<String>)

class Resolver(val interpreter: Interpreter) {
    private val errors = mutableListOf<String>()
    private val scopes = Stack<MutableMap<String, Boolean>>()

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
                declare(statement.name.type.asString)
                define(statement.name.type.asString)

                resolveFunction(statement.parameters, statement.body)
            }

            is Statement.If -> {
                resolve(statement.condition)
                resolve(statement.thenBranch)
                statement.elseBranch?.let(::resolve)
            }

            is Statement.Print -> resolve(statement.expression)
            is Statement.Return -> statement.value?.let(::resolve)
            is Statement.VarDeclaration -> {
                declare(statement.name)
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
                    errors.add("[line ${expr.name.line}] Error at ${expr.name.type}: Can't read local variable in its own initialiser.")
                }

                resolveLocal(expr, expr.name)
            }
        }
    }

    private fun resolveFunction(parameters: List<Token>, body: Statement.Block) {
        beginScope()
        parameters.forEach {
            declare(it.type.asString)
            define(it.type.asString)
        }
        resolve(body.statements)
        endScope()
    }

    private fun resolveLocal(expr: Expression, name: Token) {
        scopes.withIndex().reversed().forEach { (index, scope) ->
            if (scope.containsKey(name.type.asString)) {
                interpreter.resolve(expr, scopes.lastIndex - index)
                return
            }
        }
    }

    private fun declare(name: String) {
        if (scopes.isNotEmpty()) {
            scopes.peek()[name] = false
        }
    }

    private fun define(name: String) {
        if (scopes.isNotEmpty()) {
            scopes.peek()[name] = true
        }
    }

    private fun beginScope() = scopes.push(HashMap())
    private fun endScope() = scopes.pop()
}
