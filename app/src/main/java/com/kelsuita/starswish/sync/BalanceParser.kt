package com.starsavings.app.sync

import com.starsavings.app.data.Money

/** Conservative parser: an exact account name, a balance label and one unambiguous CNY amount. */
object BalanceParser {
    private val detailLabel = Regex("^人民币\\s*余额\\s*[:：]?\\s*(.*)$")
    private val label = Regex("^(?:当前余额|账户余额|可用余额|余额)(?:[（(](?:人民币|CNY|元)[）)])?\\s*[:：]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val amount = Regex("^[¥￥]?\\s*[+-]?\\d{1,12}(?:[,，]\\d{3})*(?:\\.\\d{1,2})?\\s*(?:元|CNY)?$", RegexOption.IGNORE_CASE)
    /** AccountDetailActivity: read the top CNY summary, never a transaction's running balance. */
    fun parseAccountDetail(lines: List<String>, expectedName: String): Long? {
        val text = lines.flatMap { it.lines() }.map { it.trim() }.filter { it.isNotEmpty() }
        if (expectedName.isBlank() || unsupported(text)) return null
        val labels = text.indices.filter { detailLabel.matches(text[it]) }
        val index = labels.singleOrNull() ?: return null
        if (text.take(index).none { it == expectedName }) return null
        // Only the value immediately next to the explicit top-summary label is eligible.
        val inline = detailLabel.matchEntire(text[index])!!.groupValues[1].trim()
        var raw = inline.ifEmpty { text.getOrNull(index + 1) ?: return null }
        val next = if (inline.isEmpty()) index + 2 else index + 1
        // Some layouts expose the whole and fractional parts as two adjacent text nodes.
        if (Regex("^[+-]?\\d{1,12}(?:[,，]\\d{3})*$").matches(raw) &&
            text.getOrNull(next)?.matches(Regex("^\\.\\d{1,2}$")) == true) raw += text[next]
        if (!amount.matches(raw)) return null
        return Money.parse(raw.replace(Regex("CNY", RegexOption.IGNORE_CASE), ""))
    }

    private fun unsupported(text: List<String>): Boolean = text.any {
        it.contains("信用额度") || it.contains("可用额度") || it.contains("美元") ||
            it.contains("港币") || it.contains("USD", true) || it.contains("HKD", true) ||
            it in setOf("总资产", "净资产", "总负债", "账户总览", "全部账户")
    }
    fun parse(lines: List<String>, expectedName: String): Long? {
        val text = lines.flatMap { it.lines() }.map { it.trim() }.filter { it.isNotEmpty() }
        if (expectedName.isBlank() || text.none { it == expectedName }) return null
        if (text.any { it.contains("信用额度") || it.contains("可用额度") ||
                    it.contains("美元") || it.contains("港币") || it.contains("USD", true) || it.contains("HKD", true) }) return null
        // Never associate a balance from an account overview or transaction list with this account.
        if (text.any { it in setOf("总资产", "净资产", "总负债", "账户总览", "全部账户") }) return null
        val candidates = mutableSetOf<Long>()
        for (index in text.indices) {
            val match = label.matchEntire(text[index]) ?: continue
            val inline = match.groupValues[1].trim()
            val raw = if (inline.isNotEmpty()) inline else text.getOrNull(index + 1) ?: continue
            if (!amount.matches(raw)) continue
            val parsed = Money.parse(raw.replace(Regex("CNY", RegexOption.IGNORE_CASE), "")) ?: continue
            candidates += parsed
        }
        return candidates.singleOrNull()
    }
}
