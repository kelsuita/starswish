package com.kelsuita.starswish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One private preference file. Every mutation is committed atomically before success is reported. */
class SavingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("savings_v1", Context.MODE_PRIVATE)

    fun hitokoto(): String? = prefs.getString("hitokoto", null)
    fun saveHitokoto(value: String) = put("hitokoto", value)

    fun appearance(): Appearance = Appearance(
        prefs.getInt("theme_mode", 0).coerceIn(0, 2),
        prefs.getInt("accent_choice", if (prefs.contains("accent_index"))
            prefs.getInt("accent_index", 0) + 2 else 0).coerceIn(0, 5),
        prefs.getInt("banner_style", 0).coerceIn(0, 1),
        prefs.getString("featured_goal", "").orEmpty(),
        prefs.getBoolean("compact_list", false),
        prefs.getInt("palette_style", 0).coerceIn(0, 8),
        prefs.getInt("color_spec", 1).coerceIn(0, 1),
        prefs.getBoolean("blur_enabled", false),
        prefs.getBoolean("pure_black_background", false)
    )
    fun saveAppearance(value: Appearance) {
        require(value.themeMode in 0..2 && value.accentIndex in 0..5 && value.bannerStyle in 0..1 &&
            value.paletteStyle in 0..8 && value.colorSpec in 0..1)
        check(prefs.edit().putInt("theme_mode", value.themeMode)
            .putInt("accent_choice", value.accentIndex)
            .putInt("banner_style", value.bannerStyle)
            .putString("featured_goal", value.featuredGoalId)
            .putBoolean("compact_list", value.compactList)
            .putInt("palette_style", value.paletteStyle)
            .putInt("color_spec", value.colorSpec)
            .putBoolean("blur_enabled", value.blurEnabled)
            .putBoolean("pure_black_background", value.pureBlackBackground).commit()) { "外观设置保存失败" }
    }

    @Synchronized fun exportBackup(): BackupData {
        val goals = goals()
        val appearance = appearance().let { value ->
            if (goals.any { it.id == value.featuredGoalId }) value else value.copy(featuredGoalId = "")
        }
        return BackupData(exportedAt = System.currentTimeMillis(), accounts = accounts(), goals = goals,
            appearance = appearance, hitokoto = hitokoto()).validate()
    }

    @Synchronized fun importBackup(backup: BackupData) {
        backup.validate()
        val value = backup.appearance
        // Commit the complete replacement once, only after every field has passed validation.
        val editor = prefs.edit()
            .putString("accounts", accountsJson(backup.accounts.mapIndexed { i, account ->
                if (account.createdAt == 0L) account.copy(createdAt = i.toLong() + 1) else account
            }).toString())
            .putString("goals", JSONArray().apply { backup.goals.forEachIndexed { i, goal ->
                put(goalJson(if (goal.createdAt == 0L) goal.copy(createdAt = i.toLong() + 1) else goal))
            } }.toString())
            .putInt("theme_mode", value.themeMode).putInt("accent_choice", value.accentIndex)
            .putInt("banner_style", value.bannerStyle).putString("featured_goal", value.featuredGoalId)
            .putBoolean("compact_list", value.compactList).putInt("palette_style", value.paletteStyle)
            .putInt("color_spec", value.colorSpec).putBoolean("blur_enabled", value.blurEnabled)
            .putBoolean("pure_black_background", value.pureBlackBackground)
            .remove("capture").remove("candidate")
        if (backup.hitokoto == null) editor.remove("hitokoto") else editor.putString("hitokoto", backup.hitokoto)
        check(editor.commit()) { "备份导入保存失败，请检查可用空间" }
    }

    @Synchronized fun accounts(): List<BankAccount> = readArray("accounts").let { array ->
        (0 until array.length()).mapNotNull { i ->
            runCatching {
                array.getJSONObject(i).let {
                    BankAccount(it.getString("id"), it.getString("name"), it.getLong("balance"),
                        it.getLong("updatedAt"), it.optString("source", "手动录入"), it.optLong("createdAt", i.toLong() + 1))
                }
            }.getOrNull()
        }
    }
    @Synchronized fun goals(): List<SavingGoal> = readArray("goals").let { array ->
        (0 until array.length()).mapNotNull { i ->
            runCatching {
                array.getJSONObject(i).let { o ->
                    val ids = o.optJSONArray("accountIds") ?: JSONArray()
                    SavingGoal(o.getString("id"), o.getString("title"), o.getLong("target"),
                        (0 until ids.length()).map { ids.getString(it) }.toSet(),
                        runCatching { GoalMode.valueOf(o.optString("mode", "BALANCE")) }
                            .getOrDefault(GoalMode.BALANCE), o.optLong("baseline"),
                        o.optString("deadline"), o.optInt("colorIndex"), o.optLong("createdAt", i.toLong() + 1))
                }
            }.getOrNull()
        }
    }
    @Synchronized fun saveAccount(account: BankAccount) {
        require(account.name.isNotBlank() && account.name.length <= 40)
        require(account.balance in -Money.MAX_CENTS..Money.MAX_CENTS)
        val existing = accounts()
        require(existing.none { it.id != account.id && it.name == account.name }) { "账户名称已存在" }
        require(existing.any { it.id == account.id } || existing.size < 50) { "最多添加 50 个账户" }
        writeAccounts(existing.map { if (it.id == account.id) account.copy(createdAt = it.createdAt) else it }
            .let { if (existing.any { it.id == account.id }) it else it + account.copy(createdAt = System.currentTimeMillis()) })
    }
    @Synchronized fun saveGoal(goal: SavingGoal) {
        require(goal.title.isNotBlank() && goal.title.length <= 40)
        require(goal.target in 1..Money.MAX_CENTS && goal.accountIds.isNotEmpty())
        require(goal.accountIds.all { id -> accounts().any { it.id == id } })
        val existing = goals()
        require(existing.any { it.id == goal.id } || existing.size < 100) { "最多添加 100 个目标" }
        put("goals", JSONArray().apply {
            existing.map { if (it.id == goal.id) goal.copy(createdAt = it.createdAt) else it }
                .let { if (existing.any { it.id == goal.id }) it else it + goal.copy(createdAt = System.currentTimeMillis()) }
                .forEach { put(goalJson(it)) }
        }.toString())
    }
    @Synchronized fun reorderGoals(ids: List<String>) {
        val current = goals()
        require(ids.size == current.size && ids.toSet().size == ids.size && ids.toSet() == current.map { it.id }.toSet())
        val byId = current.associateBy { it.id }
        put("goals", JSONArray().apply { ids.forEach { put(goalJson(byId.getValue(it))) } }.toString())
    }
    @Synchronized fun reorderAccounts(ids: List<String>) {
        val current = accounts()
        require(ids.size == current.size && ids.toSet().size == ids.size && ids.toSet() == current.map { it.id }.toSet())
        val byId = current.associateBy { it.id }
        writeAccounts(ids.map { byId.getValue(it) })
    }
    @Synchronized fun deleteGoal(id: String) {
        val remaining = goals().filterNot { it.id == id }
        val editor = prefs.edit()
            .putString("goals", JSONArray().apply { remaining.forEach { put(goalJson(it)) } }.toString())
        // A dangling featured id would leave the home banner falling back to the first goal,
        // while settings still highlighted a missing entry.
        if (prefs.getString("featured_goal", "") == id) editor.putString("featured_goal", "")
        check(editor.commit()) { "保存失败，请检查可用空间" }
    }
    @Synchronized fun deleteAccount(id: String) {
        require(goals().none { id in it.accountIds }) { "请先在关联目标中解除该账户" }
        writeAccounts(accounts().filterNot { it.id == id })
        if (candidate()?.accountId == id) clearCandidate()
        if (request()?.accountId == id) cancelCapture()
    }
    fun request(): CaptureRequest? = runCatching {
        prefs.getString("capture", null)?.let {
            JSONObject(it).let { o -> CaptureRequest(o.getString("accountId"), o.getLong("expiresAt")) }
        }
    }.getOrNull()
    fun startCapture(id: String) {
        require(accounts().any { it.id == id })
        check(prefs.edit().remove("candidate").putString("capture", JSONObject()
            .put("accountId", id).put("expiresAt", System.currentTimeMillis() + 120_000).toString()).commit())
    }
    fun cancelCapture() { check(prefs.edit().remove("capture").commit()) }
    fun candidate(): BalanceCandidate? = runCatching {
        prefs.getString("candidate", null)?.let {
            JSONObject(it).let { o -> BalanceCandidate(o.getString("accountId"), o.getLong("balance"), o.getLong("capturedAt")) }
        }
    }.getOrNull()
    fun stage(candidate: BalanceCandidate) {
        check(prefs.edit().putString("candidate", JSONObject().put("accountId", candidate.accountId)
            .put("balance", candidate.balance).put("capturedAt", candidate.capturedAt).toString())
            .remove("capture").commit())
    }
    fun clearCandidate() { check(prefs.edit().remove("candidate").commit()) }
    @Synchronized fun acceptCandidate() {
        val candidate = candidate() ?: return
        val account = accounts().firstOrNull { it.id == candidate.accountId } ?: error("该账户已删除")
        val updated = accounts().map {
            if (it.id == account.id) it.copy(balance = candidate.balance, updatedAt = candidate.capturedAt,
                source = "小星页面识别 · 已确认") else it
        }
        check(prefs.edit().putString("accounts", accountsJson(updated).toString()).remove("candidate").commit())
    }
    private fun readArray(key: String): JSONArray = runCatching { JSONArray(prefs.getString(key, "[]")) }
        .getOrElse { JSONArray() }
    private fun put(key: String, value: String) { check(prefs.edit().putString(key, value).commit()) { "保存失败，请检查可用空间" } }
    private fun writeAccounts(accounts: List<BankAccount>) = put("accounts", accountsJson(accounts).toString())
    private fun accountsJson(accounts: List<BankAccount>) = JSONArray().apply {
        accounts.forEach { put(JSONObject().put("id", it.id).put("name", it.name)
            .put("balance", it.balance).put("updatedAt", it.updatedAt).put("source", it.source)
            .put("createdAt", it.createdAt)) }
    }
    private fun goalJson(goal: SavingGoal) = JSONObject().put("id", goal.id).put("title", goal.title)
        .put("target", goal.target).put("accountIds", JSONArray(goal.accountIds.toList()))
        .put("mode", goal.mode.name).put("baseline", goal.baseline).put("deadline", goal.deadline)
        .put("colorIndex", goal.colorIndex).put("createdAt", goal.createdAt)
}
