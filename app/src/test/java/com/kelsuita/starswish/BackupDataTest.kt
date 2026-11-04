package com.starsavings.app

import com.starsavings.app.data.*
import org.junit.Assert.*
import org.junit.Test

class BackupDataTest {
    private fun backup() = BackupData(exportedAt = 1,
        accounts = listOf(BankAccount("account", "储蓄账户", 123456, 1)),
        goals = listOf(SavingGoal("goal", "旅行", 500000, setOf("account"), GoalMode.GROWTH, 100000, "2027-01-01")),
        appearance = Appearance(themeMode = 2, featuredGoalId = "goal", pureBlackBackground = true),
        hitokoto = "慢慢来，比较快。")

    @Test fun roundTripPreservesDataAndMoney() {
        val original = backup()
        assertEquals(original, BackupData.fromJson(original.toJson()))
        assertEquals(original, BackupData.fromJson("\uFEFF" + original.toJson()))
    }

    @Test fun emptyBackupIsValid() {
        val empty = BackupData(exportedAt = 0, accounts = emptyList(), goals = emptyList(), appearance = Appearance())
        assertEquals(empty, BackupData.fromJson(empty.toJson()))
    }

    @Test fun caseDistinctAccountNamesCanBeBackedUp() {
        val original = backup().copy(accounts = listOf(
            BankAccount("account", "Savings", 100, 1),
            BankAccount("other", "savings", 200, 1)))
        assertEquals(original, BackupData.fromJson(original.toJson()))
    }

    @Test fun legacyBackupWithoutCreationDatesStillImports() {
        val old = backup().toJson().replace(Regex(""",\s*"createdAt"\s*:\s*0"""), "")
        assertFalse(old.contains("createdAt"))
        val decoded = BackupData.fromJson(old)
        assertEquals(0L, decoded.accounts.single().createdAt)
        assertEquals(0L, decoded.goals.single().createdAt)
    }

    @Test fun creationDatesSurviveExportAndImport() {
        val original = backup().copy(
            accounts = backup().accounts.map { it.copy(createdAt = 123L) },
            goals = backup().goals.map { it.copy(createdAt = 456L) },
        )
        assertEquals(original, BackupData.fromJson(original.toJson()))
        assertThrows(IllegalArgumentException::class.java) {
            original.copy(goals = original.goals.map { it.copy(createdAt = -1L) }).toJson()
        }
    }

    @Test fun invalidBackupsAreRejected() {
        val original = backup()
        listOf(
            original.copy(version = 2),
            original.copy(format = "another-app"),
            original.copy(accounts = original.accounts + original.accounts),
            original.copy(accounts = emptyList()),
            original.copy(goals = listOf(original.goals.first().copy(target = 0))),
            original.copy(goals = listOf(original.goals.first().copy(deadline = "2027-13-30"))),
            original.copy(appearance = Appearance(accentIndex = 10)),
            original.copy(appearance = Appearance(featuredGoalId = "missing")),
        ).forEach { value -> assertThrows(IllegalArgumentException::class.java) { value.toJson() } }
        assertThrows(IllegalArgumentException::class.java) { BackupData.fromJson("{}") }
        assertThrows(IllegalArgumentException::class.java) { BackupData.fromJson("x".repeat(BackupData.MAX_BYTES + 1)) }
    }
}
