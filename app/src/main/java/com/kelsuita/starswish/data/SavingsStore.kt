package com.kelsuita.starswish.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One private preference file. Writes update memory immediately and flush to disk asynchronously. */
class SavingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("savings_v1", Context.MODE_PRIVATE)

    fun developerModeEnabled(): Boolean = prefs.getBoolean("developer_mode", false)
    fun setDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("developer_mode", enabled).apply()
    }
    fun webDavBalanceEnabled(): Boolean = prefs.getBoolean("webdav_balance_enabled", false)
    fun setWebDavBalanceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("webdav_balance_enabled", enabled).apply()
    }
    fun webDavUrl(): String = prefs.getString("webdav_url", "").orEmpty()
    fun webDavAccount(): String = prefs.getString("webdav_account", "").orEmpty()
    fun encryptedWebDavPassword(): String = prefs.getString("webdav_password", "").orEmpty()
    /** Connected means saved credentials plus at least one confirmed balance import. */
    fun webDavConnected(): Boolean = prefs.getBoolean("webdav_connected", false) &&
        webDavAccount().isNotBlank() && encryptedWebDavPassword().isNotBlank()
    fun markWebDavConnected() {
        prefs.edit().putBoolean("webdav_connected", true).apply()
    }
    fun markWebDavDisconnected() {
        prefs.edit().putBoolean("webdav_connected", false).apply()
    }
    /**
     * Saves credentials. [credentialsChanged] is decided by the caller because the stored
     * password is encrypted with a random IV, so comparing ciphertext would always report a
     * change even when the plaintext is identical.
     */
    fun saveWebDavSettings(url: String, account: String, encryptedPassword: String, credentialsChanged: Boolean) {
        val editor = prefs.edit().putString("webdav_url", url.trim())
            .putString("webdav_account", account.trim())
            .putString("webdav_password", encryptedPassword)
        if (credentialsChanged) editor.putBoolean("webdav_connected", false)
        editor.apply()
    }

    fun hitokoto(): String? = prefs.getString("hitokoto", null)
    fun saveHitokoto(value: String) = put("hitokoto", value)

    /**
     * Quote (一言) choice. Null means the user has not chosen yet, so the effective default
     * follows the UI language (on for Chinese). A stored value is the user's explicit choice and
     * is preserved when the language changes; non-Chinese UI hides the quote regardless.
     */
    fun quoteEnabled(): Boolean? =
        if (prefs.contains("quote_enabled")) prefs.getBoolean("quote_enabled", true) else null
    fun saveQuoteEnabled(value: Boolean) {
        prefs.edit().putBoolean("quote_enabled", value).apply()
    }

    /** UI language index: an index into [AppLanguage.entries]. */
    fun language(): Int {
        if (prefs.contains("language_v2")) return prefs.getInt("language_v2", 0).coerceIn(0, AppLanguage.entries.lastIndex)
        // Builds before the follow-system option stored 0..2 directly; shift those by one so an
        // explicit choice survives the update instead of silently becoming "follow system".
        val legacy = if (prefs.contains("language")) prefs.getInt("language", 1) else -1
        return if (legacy in 0..2) legacy + 1 else 0
    }
    fun saveLanguage(value: Int) {
        require(value in AppLanguage.entries.indices)
        prefs.edit().putInt("language_v2", value).apply()
    }

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
        prefs.getBoolean("pure_black_background", false),
        prefs.getBoolean("floating_nav_bar", false),
        prefs.getInt("floating_nav_bar_style", 0).coerceIn(0, 1),
        prefs.getBoolean("predictive_back_enabled", true),
        prefs.getInt("banner_decimals", 0).coerceIn(0, 5)
    )
    fun saveAppearance(value: Appearance) {
        require(value.themeMode in 0..2 && value.accentIndex in 0..5 && value.bannerStyle in 0..1 &&
            value.paletteStyle in 0..8 && value.colorSpec in 0..1 && value.floatingNavBarStyle in 0..1 &&
            value.bannerDecimals in 0..5)
        prefs.edit().putInt("theme_mode", value.themeMode)
            .putInt("accent_choice", value.accentIndex)
            .putInt("banner_style", value.bannerStyle)
            .putString("featured_goal", value.featuredGoalId)
            .putBoolean("compact_list", value.compactList)
            .putInt("palette_style", value.paletteStyle)
            .putInt("color_spec", value.colorSpec)
            .putBoolean("blur_enabled", value.blurEnabled)
            .putBoolean("pure_black_background", value.pureBlackBackground)
            .putBoolean("floating_nav_bar", value.floatingNavBar)
            .putInt("floating_nav_bar_style", value.floatingNavBarStyle)
            .putBoolean("predictive_back_enabled", value.predictiveBackEnabled)
            .putInt("banner_decimals", value.bannerDecimals).apply()
    }

    @Synchronized fun exportBackup(): BackupData {
        val goals = goals()
        val appearance = appearance().let { value ->
            if (goals.any { it.id == value.featuredGoalId }) value else value.copy(featuredGoalId = "")
        }
        return BackupData(exportedAt = System.currentTimeMillis(), accounts = accounts(), goals = goals,
            appearance = appearance, hitokoto = hitokoto(), language = language(),
            quoteEnabled = quoteEnabled()).validate()
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
            .putBoolean("floating_nav_bar", value.floatingNavBar)
            .putInt("floating_nav_bar_style", value.floatingNavBarStyle)
            .putBoolean("predictive_back_enabled", value.predictiveBackEnabled)
            .putInt("banner_decimals", value.bannerDecimals)
            .putInt("language_v2", backup.language)
            .remove("capture").remove("candidate")
            // Credentials belong to this device, not to the imported backup.
            .remove("webdav_url").remove("webdav_account").remove("webdav_password")
            .remove("webdav_connected")
        if (backup.quoteEnabled == null) editor.remove("quote_enabled")
        else editor.putBoolean("quote_enabled", backup.quoteEnabled)
        if (backup.hitokoto == null) editor.remove("hitokoto") else editor.putString("hitokoto", backup.hitokoto)
        check(editor.commit()) { tr("backup.error_import_save") }
    }

    @Synchronized fun accounts(): List<BankAccount> = readArray("accounts").let { array ->
        (0 until array.length()).mapNotNull { i ->
            runCatching {
                array.getJSONObject(i).let {
                    BankAccount(it.getString("id"), it.getString("name"), it.getLong("balance"),
                        it.getLong("updatedAt"), it.optString("source", "account.source_manual"), it.optLong("createdAt", i.toLong() + 1))
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
        require(existing.none { it.id != account.id && it.name == account.name }) { tr("account.error_name_exists") }
        require(existing.any { it.id == account.id } || existing.size < 50) { tr("account.error_max_accounts") }
        writeAccounts(existing.map { if (it.id == account.id) account.copy(createdAt = it.createdAt) else it }
            .let { if (existing.any { it.id == account.id }) it else it + account.copy(createdAt = System.currentTimeMillis()) })
    }
    @Synchronized fun saveAccountBalances(balances: Map<String, Long>, updatedAt: Long) {
        val existing = accounts()
        require(balances.isNotEmpty() && balances.keys.all { id -> existing.any { it.id == id } })
        require(balances.values.all { it in -Money.MAX_CENTS..Money.MAX_CENTS })
        val updated = existing.map { account ->
            balances[account.id]?.let { cents ->
                account.copy(balance = cents, updatedAt = updatedAt, source = "account.source_webdav")
            } ?: account
        }
        writeAccounts(updated)
    }
    @Synchronized fun saveGoal(goal: SavingGoal) {
        require(goal.title.isNotBlank() && goal.title.length <= 40)
        require(goal.target in 1..Money.MAX_CENTS && goal.accountIds.isNotEmpty())
        require(goal.accountIds.all { id -> accounts().any { it.id == id } })
        val existing = goals()
        require(existing.any { it.id == goal.id } || existing.size < 100) { tr("goal.error_max_goals") }
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
        editor.apply()
    }
    @Synchronized fun deleteAccount(id: String) {
        require(goals().none { id in it.accountIds }) { tr("account.error_unlink_first") }
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
        prefs.edit().remove("candidate").putString("capture", JSONObject()
            .put("accountId", id).put("expiresAt", System.currentTimeMillis() + 120_000).toString()).apply()
    }
    fun cancelCapture() { prefs.edit().remove("capture").apply() }
    fun candidate(): BalanceCandidate? = runCatching {
        prefs.getString("candidate", null)?.let {
            JSONObject(it).let { o -> BalanceCandidate(o.getString("accountId"), o.getLong("balance"), o.getLong("capturedAt")) }
        }
    }.getOrNull()
    fun stage(candidate: BalanceCandidate) {
        prefs.edit().putString("candidate", JSONObject().put("accountId", candidate.accountId)
            .put("balance", candidate.balance).put("capturedAt", candidate.capturedAt).toString())
            .remove("capture").apply()
    }
    fun clearCandidate() { prefs.edit().remove("candidate").apply() }
    @Synchronized fun acceptCandidate() {
        val candidate = candidate() ?: return
        require(candidate.balance in -Money.MAX_CENTS..Money.MAX_CENTS)
        val account = accounts().firstOrNull { it.id == candidate.accountId } ?: error(tr("account.error_deleted"))
        val updated = accounts().map {
            if (it.id == account.id) it.copy(balance = candidate.balance, updatedAt = candidate.capturedAt,
                source = "account.source_xiaoxing") else it
        }
        prefs.edit().putString("accounts", accountsJson(updated).toString()).remove("candidate").apply()
    }
    private fun readArray(key: String): JSONArray = runCatching { JSONArray(prefs.getString(key, "[]")) }
        .getOrElse { JSONArray() }
    private fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
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
