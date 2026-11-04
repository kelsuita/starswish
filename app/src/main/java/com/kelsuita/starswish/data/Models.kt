package com.kelsuita.starswish.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable

object Money {
    const val MAX_CENTS = 99_999_999_999_999L
    fun parse(text: String): Long? {
        val clean = text.trim().replace(",", "").replace("，", "")
            .replace("￥", "").replace("¥", "").removeSuffix("元").trim()
        if (!Regex("[+-]?\\d{1,12}(\\.\\d{1,2})?").matches(clean)) return null
        return runCatching {
            BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact().takeIf { it in -MAX_CENTS..MAX_CENTS }
        }.getOrNull()
    }
    fun input(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
    fun format(cents: Long): String = NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(cents, 2))
}

@Serializable
data class BankAccount(
    val id: String,
    val name: String,
    val balance: Long,
    val updatedAt: Long,
    val source: String = "手动录入",
    val createdAt: Long = 0L,
)

@Serializable
enum class GoalMode { BALANCE, GROWTH }

@Serializable
data class SavingGoal(
    val id: String,
    val title: String,
    val target: Long,
    val accountIds: Set<String>,
    val mode: GoalMode = GoalMode.BALANCE,
    val baseline: Long = 0L,
    val deadline: String = "",
    val colorIndex: Int = 0,
    val createdAt: Long = 0L,
) {
    fun saved(accounts: List<BankAccount>): Long =
        (accounts.filter { it.id in accountIds }.sumOf { it.balance } -
            if (mode == GoalMode.GROWTH) baseline else 0L).coerceAtLeast(0L)
    fun progress(accounts: List<BankAccount>): Float =
        if (target <= 0) 0f else (saved(accounts).toDouble() / target).toFloat().coerceIn(0f, 1f)
    fun remaining(accounts: List<BankAccount>): Long = (target - saved(accounts)).coerceAtLeast(0L)
}

data class BalanceCandidate(val accountId: String, val balance: Long, val capturedAt: Long)
data class CaptureRequest(val accountId: String, val expiresAt: Long)
