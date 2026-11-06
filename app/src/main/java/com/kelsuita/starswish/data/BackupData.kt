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
) {
    fun validate(): BackupData {
        require(format == "starswish-backup" && version == 1) { tr("不是支持的星愿备份文件或备份版本") }
        require(exportedAt >= 0 && accounts.size <= 50 && goals.size <= 100) { tr("备份数据超出限制") }
        require(accounts.map { it.id }.distinct().size == accounts.size &&
            accounts.map { it.name }.distinct().size == accounts.size &&
            goals.map { it.id }.distinct().size == goals.size) { tr("备份包含重复的账户或目标") }
        accounts.forEach {
            require(it.id.isNotBlank() && it.id.length <= 128 && it.name.isNotBlank() && it.name.length <= 40 &&
                it.balance in -Money.MAX_CENTS..Money.MAX_CENTS && it.updatedAt >= 0 && it.createdAt >= 0 && it.source.length <= 200) {
                tr("备份中的账户数据无效")
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
                tr("备份中的目标或关联账户无效")
            }
        }
        require(appearance.themeMode in 0..2 && appearance.accentIndex in 0..5 &&
            appearance.bannerStyle in 0..1 && appearance.paletteStyle in 0..8 && appearance.colorSpec in 0..1 &&
            (appearance.featuredGoalId.isEmpty() || appearance.featuredGoalId.length <= 128) &&
            (appearance.featuredGoalId.isEmpty() || goals.any { it.id == appearance.featuredGoalId })) {
            tr("备份中的外观设置无效")
        }
        require(hitokoto == null || (hitokoto.isNotBlank() && hitokoto.length <= 8192)) { tr("备份中的一言无效") }
        return this
    }

    fun toJson(): String = codec.encodeToString(validate())

    companion object {
        const val MAX_BYTES = 1024 * 1024
        private val codec = Json { prettyPrint = true; encodeDefaults = true }

        fun fromJson(text: String): BackupData {
            require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { tr("备份文件不能超过 1 MB") }
            val decoded = try {
                codec.decodeFromString<BackupData>(text.removePrefix("\uFEFF"))
            } catch (error: SerializationException) {
                throw IllegalArgumentException(tr("备份格式或内容无效，请选择星愿导出的 JSON 文件"), error)
            }
            // Validation messages are already written for the user; do not replace them with
            // the generic parse error above.
            return decoded.validate()
        }
    }
}
