package com.starsavings.app

import com.starsavings.app.data.*
import com.starsavings.app.sync.BalanceParser
import org.junit.Assert.*
import org.junit.Test

class SavingsLogicTest {
    @Test fun detailReadsSummaryInsteadOfTransactionBalances() {
        val page = listOf("招商银行万事网联卡", "3597", "编辑", "人民币余额", "586.42",
            "流入", "4,074.81", "流出", "4,488.27", "2026-11", "利息", "+0.01",
            "余额 586.42", "利息", "+0.01", "余额 586.41", "余额 436.40")
        assertEquals(58642L, BalanceParser.parseAccountDetail(page, "招商银行万事网联卡"))
        assertNull(BalanceParser.parseAccountDetail(page, "其他账户"))
        assertNull(BalanceParser.parseAccountDetail(page.filterNot { it == "586.42" }, "招商银行万事网联卡"))
    }
    @Test fun detailHandlesSplitDecimalAndRejectsMissingOrAmbiguousSummary() {
        assertEquals(58642L, BalanceParser.parseAccountDetail(listOf("储蓄卡", "人民币余额", "586", ".42"), "储蓄卡"))
        assertEquals(0L, BalanceParser.parseAccountDetail(listOf("储蓄卡", "人民币余额：0.00"), "储蓄卡"))
        assertNull(BalanceParser.parseAccountDetail(listOf("储蓄卡", "余额 586.41"), "储蓄卡"))
        assertNull(BalanceParser.parseAccountDetail(listOf("储蓄卡", "人民币余额", "***"), "储蓄卡"))
        assertNull(BalanceParser.parseAccountDetail(listOf("储蓄卡", "人民币余额", "1", "人民币余额", "2"), "储蓄卡"))
        assertNull(BalanceParser.parseAccountDetail(listOf("储蓄卡", "可用额度", "人民币余额", "100"), "储蓄卡"))
    }
    private val account = BankAccount("bank", "招商储蓄", 123456, 0)
    @Test fun moneyUsesExactCents() {
        assertEquals(123456L, Money.parse("￥1,234.56"))
        assertEquals(10L, Money.parse("0.10"))
        assertEquals(-101L, Money.parse("-1.01"))
        assertNull(Money.parse("1.001"))
        assertNull(Money.parse("NaN"))
        assertNull(Money.parse("1e6"))
        assertNull(Money.parse("1000000000000"))
    }
    @Test fun progressIncludesOnlyBoundAccounts() {
        val goal = SavingGoal("goal", "去旅行", 200000, setOf("bank"))
        assertEquals(123456L, goal.saved(listOf(account, account.copy(id = "other"))))
        assertEquals(.61728f, goal.progress(listOf(account)), .00001f)
        assertEquals(76544L, goal.remaining(listOf(account)))
    }
    @Test fun growthAndNegativeBalancesAreClamped() {
        val goal = SavingGoal("goal", "应急金", 10000, setOf("bank"), GoalMode.GROWTH, 120000)
        assertEquals(3456L, goal.saved(listOf(account)))
        assertEquals(0f, goal.progress(listOf(account.copy(balance = -500))), 0f)
        assertEquals(1f, goal.progress(listOf(account.copy(balance = 999999))), 0f)
        assertEquals(0L, goal.remaining(listOf(account.copy(balance = 999999))))
    }
    @Test fun parserRequiresAccountAndBalanceLabel() {
        assertEquals(123456L, BalanceParser.parse(listOf("招商储蓄", "账户余额", "¥1,234.56"), "招商储蓄"))
        assertEquals(123456L, BalanceParser.parse(listOf("招商储蓄", "当前余额：1234.56元"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("其他账户", "余额", "1234.56"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "1234.56"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "账户余额", "****1234"), "招商储蓄"))
    }
    @Test fun parserRejectsAmbiguityCreditAndForeignCurrencies() {
        assertNull(BalanceParser.parse(listOf("招商储蓄", "余额：123", "可用余额：120"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "可用额度", "余额：123"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "USD", "余额：123"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "总资产", "余额：123"), "招商储蓄"))
    }
    @Test fun parserSupportsNegativeAndZeroButNotTransactions() {
        assertEquals(-100L, BalanceParser.parse(listOf("招商储蓄", "余额：-1.00"), "招商储蓄"))
        assertEquals(0L, BalanceParser.parse(listOf("招商储蓄", "余额", "0.00"), "招商储蓄"))
        assertNull(BalanceParser.parse(listOf("招商储蓄", "收入", "1000", "支出", "200"), "招商储蓄"))
    }
}
