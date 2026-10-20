package com.example.financecalc1

/** Groups digits without rounding or changing the user's decimal separator. */
object NumberEditing {
    data class Edit(val text: String, val caret: Int)
    private fun separator(c: Char) = c == ' ' || c == '\u00a0' || c == '\u202f'
    fun rateFraction(source: String): String? = source.filterNot(::separator).replace(',', '.')
        .toBigDecimalOrNull()?.movePointLeft(2)?.stripTrailingZeros()?.toPlainString()?.replace('.', ',')
    fun group(source: String): String {
        val raw = source.filterNot(::separator)
        if (!Regex("-?[0-9]*([.,][0-9]*)?").matches(raw)) return source
        val sign = if (raw.startsWith('-')) "-" else ""
        val unsigned = raw.removePrefix("-")
        val boundary = unsigned.indexOfFirst { it == ',' || it == '.' }.let { if (it < 0) unsigned.length else it }
        val integer = unsigned.take(boundary)
        val grouped = integer.reversed().chunked(3).joinToString(" ").reversed()
        return sign + grouped + unsigned.substring(boundary)
    }
    fun update(source: String, caret: Int, previous: String = "", start: Int = 0, removed: Int = 0, inserted: Int = 0, previousCaret: Int = caret): Edit {
        var value = source
        var position = caret.coerceIn(0, source.length)
        // Deleting only a grouping space should delete a digit, rather than reinsert the space forever.
        if (removed > 0 && inserted == 0 && start + removed <= previous.length &&
            previous.substring(start, start + removed).all(::separator)) {
            val index = if (previousCaret > start) start - 1 else start
            if (index in value.indices && value[index].isDigit()) {
                value = value.removeRange(index, index + 1)
                if (position > index) position--
            }
        }
        val before = value.take(position).count { !separator(it) }
        val grouped = group(value)
        var offset = 0; var seen = 0
        while (offset < grouped.length && seen < before) {
            if (!separator(grouped[offset])) seen++
            offset++
        }
        return Edit(grouped, offset)
    }
    fun backspaceRange(text: String, caret: Int): IntRange? {
        if (caret <= 0) return null
        var index = (caret - 1).coerceAtMost(text.lastIndex)
        while (index > 0 && separator(text[index])) index--
        return index until caret
    }
}
