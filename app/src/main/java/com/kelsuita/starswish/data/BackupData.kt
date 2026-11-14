package com.kelsuita.starswish.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class BackupData(
    val format: String = "starswish-backup",
    val version: Int = 1,
    val exportedAt: Long,
    val accounts: List<BankAccount>,
    val goals: List<SavingGoal>,
    val appearance: Appearance,
    val hitokoto: String? = null,
    /** Stored language choice; an index into [AppLanguage.entries]. */
    val language: Int = 0,
    /** Null means no manual quote choice; the language default then applies. */
    val quoteEnabled: Boolean? = null,
) {
    fun validate(): BackupData {
        require(format == "starswish-backup" && version == 1) { tr("backup.error_unsupported") }
        require(exportedAt >= 0 && accounts.size <= 50 && goals.size <= 100) { tr("backup.error_too_large") }
        require(accounts.map { it.id }.distinct().size == accounts.size &&
            accounts.map { it.name }.distinct().size == accounts.size &&
            goals.map { it.id }.distinct().size == goals.size) { tr("backup.error_duplicates") }
        accounts.forEach {
            require(it.id.isNotBlank() && it.id.length <= 128 && it.name.isNotBlank() && it.name.length <= 40 &&
                it.balance in -Money.MAX_CENTS..Money.MAX_CENTS && it.updatedAt >= 0 && it.createdAt >= 0 && it.source.length <= 200) {
                tr("backup.error_accounts")
            }
        }
        val accountIds = accounts.map { it.id }.toSet()
        goals.forEach {
            require(it.id.isNotBlank() && it.id.length <= 128 && it.title.isNotBlank() && it.title.length <= 40 &&
                it.target in 1..Money.MAX_CENTS && it.accountIds.isNotEmpty() &&
                it.accountIds.size <= accounts.size &&
                it.accountIds.all { id -> id in accountIds } &&
                it.baseline in -(50 * Money.MAX_CENTS)..(50 * Money.MAX_CENTS) &&
                it.colorIndex >= 0 && it.createdAt >= 0 && (it.deadline.isEmpty() ||
                (it.deadline.length == 10 && runCatching { LocalDate.parse(it.deadline) }.isSuccess))) {
                tr("backup.error_goal_or_account")
            }
        }
        require(appearance.themeMode in 0..2 && appearance.accentIndex in 0..5 &&
            appearance.bannerStyle in 0..1 && appearance.paletteStyle in 0..8 && appearance.colorSpec in 0..1 &&
            appearance.floatingNavBarStyle in 0..1 &&
            appearance.bannerDecimals in 0..5 &&
            (appearance.featuredGoalId.isEmpty() || appearance.featuredGoalId.length <= 128) &&
            (appearance.featuredGoalId.isEmpty() || goals.any { it.id == appearance.featuredGoalId })) {
            tr("backup.error_appearance")
        }
        require(language in AppLanguage.entries.indices) { tr("backup.error_settings") }
        require(hitokoto == null || (hitokoto.isNotBlank() && hitokoto.length <= 8192)) { tr("backup.error_quote") }
        return this
    }

    fun toJson(): String = codec.encodeToString(validate())

    companion object {
        const val MAX_BYTES = 1024 * 1024
        private val codec = Json { prettyPrint = true; encodeDefaults = true }

        fun fromJson(text: String): BackupData {
            require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { tr("backup.error_over_1mb") }
            val decoded = try {
                codec.decodeFromString<BackupData>(text.removePrefix("\uFEFF"))
            } catch (error: SerializationException) {
                throw IllegalArgumentException(tr("backup.error_invalid_file"), error)
            }
            // Validation messages are already written for the user; do not replace them with
            // the generic parse error above.
            return decoded.validate()
        }
    }
}
