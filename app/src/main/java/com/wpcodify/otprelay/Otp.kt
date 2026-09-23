package com.wpcodify.otprelay

object Otp {
    private val map = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9
    )
    private val runRe = Regex(
        """\b(?:zero|one|two|three|four|five|six|seven|eight|nine)(?:[\s,\-]+(?:zero|one|two|three|four|five|six|seven|eight|nine)){3,}\b"""
    )
    private val wordRe = Regex("zero|one|two|three|four|five|six|seven|eight|nine")
    private val digitRe = Regex("""\b\d{4,8}\b""")

    /** "For security ... Nine-Eight-One-Four-Eight-Five" -> "981485"; also handles plain digits. */
    fun extract(text: String): String? {
        val t = text.lowercase()
        val run = runRe.findAll(t).map { it.value }.maxByOrNull { it.length }
        if (run != null) {
            return wordRe.findAll(run).map { map[it.value] }.joinToString("")
        }
        return digitRe.find(text)?.value
    }
}
