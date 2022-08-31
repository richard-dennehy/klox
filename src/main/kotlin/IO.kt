interface IO {
    fun print(text: String)
}

class SystemIO: IO {
    override fun print(text: String) = println(text)
}
