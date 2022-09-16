sealed class LoxValue(val asString: kotlin.String) {
    open val isTruthy: Boolean = true

    object Nil : LoxValue("nil") {
        override val isTruthy: Boolean = false
    }

    object True : LoxValue("true")
    object False : LoxValue("false") {
        override val isTruthy: Boolean = false
    }

    data class String(val value: kotlin.String) : LoxValue("\"$value\"")
    data class Number(val value: Double) : LoxValue(value.toString().removeSuffix(".0"))
    class Function(name: kotlin.String, val arity: Int, val closure: Scope, val call: (Interpreter, Scope, List<LoxValue>) -> LoxValue): LoxValue("fn <$name>")

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
            is TokenType.LoxString -> String(literal.asString)
        }
    }
}
