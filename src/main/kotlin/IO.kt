interface IO {
    fun print(text: String)
    fun currentTime(): Double
}

class SystemIO: IO {
    override fun print(text: String) = println(text)
    override fun currentTime(): Double = System.currentTimeMillis() / 1000.0
}
