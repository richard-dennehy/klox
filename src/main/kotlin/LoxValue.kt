sealed class LoxValue(val asString: String) {
    open val isTruthy: Boolean = true

    object Nil : LoxValue("nil") {
        override val isTruthy: Boolean = false
    }

    object True : LoxValue("true")
    object False : LoxValue("false") {
        override val isTruthy: Boolean = false
    }

    data class LoxString(val value: String) : LoxValue("\"$value\"")
    data class Number(val value: Double) : LoxValue(value.toString().removeSuffix(".0"))
    class Function(
        private val name: String,
        val arity: Int,
        val closure: Scope,
        val call: (Interpreter, Scope, List<LoxValue>) -> LoxValue
    ) : LoxValue("fn <$name>") {
        internal fun bind(instance: LoxInstance): Function {
            val scope = Scope(closure.builtins, closure)
            scope.define(instance)
            return Function(name, arity, scope, call)
        }
    }

    class LoxClass(name: String, internal val instanceMethods: Map<String, Function>, val classMethods: Map<String, Function>) : LoxValue(name) {
        operator fun invoke(interpreter: Interpreter, arguments: List<LoxValue>): LoxInstance {
            val instance = LoxInstance(this, mutableMapOf())
            val initialiser = instanceMethods["init"]
            val bound = initialiser?.bind(instance)
            bound?.call?.invoke(interpreter, bound.closure, arguments)
            return instance
        }

        val arity = instanceMethods["init"]?.arity ?: 0
    }

    class LoxInstance(private val klass: LoxClass, private val fields: MutableMap<String, LoxValue>) :
        LoxValue(klass.asString + " instance") {
        operator fun get(name: String): LoxValue? {
            val field = fields[name]
            if (field != null) return field

            val method = klass.instanceMethods[name]
            return method?.bind(this)
        }

        operator fun set(name: String, value: LoxValue) {
            fields[name] = value
        }
    }

    companion object {
        fun from(value: Boolean): LoxValue = if (value) {
            True
        } else {
            False
        }

        fun from(literal: TokenType.Literal): LoxValue = when (literal) {
            TokenType.KeywordLiteral.False -> False
            TokenType.KeywordLiteral.Nil -> Nil
            TokenType.KeywordLiteral.True -> True
            is TokenType.LoxNumber -> Number(literal.asDouble)
            is TokenType.LoxString -> LoxString(literal.asString)
        }
    }
}
