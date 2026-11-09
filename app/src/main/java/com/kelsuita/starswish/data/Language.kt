package com.kelsuita.starswish.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class AppLanguage(val nativeName: String, val pack: String) {
    /** First-launch default: use whatever language the system is set to. */
    FollowSystem("跟随系统", ""),
    SimplifiedChinese("简体中文", "zh-CN"),
    TraditionalChinese("繁體中文", "zh-TW"),
    English("English", "en"),
    Japanese("日本語", "ja"),
    ;

    /** Chinese copy is the only place the quote (一言) is offered. */
    val isChinese: Boolean get() = this == SimplifiedChinese || this == TraditionalChinese
}

/**
 * Every UI string is addressed by a stable identifier such as `button.submit`; the copy itself
 * lives in the JSON files under `assets/i18n/` so translators (and Crowdin) only touch those files.
 *
 * [current] is snapshot state, so the lookup inside [tr] is tracked by composition and switching
 * languages recomposes the screens that read it; the switch takes effect immediately without
 * recreating the activity. [attach] must run from Application.onCreate before any lookup.
 */
object Languages {
    /** The stored choice, which may be [AppLanguage.FollowSystem]. */
    var choice by mutableStateOf(AppLanguage.FollowSystem)
        private set

    /** The language actually used for lookups, with [AppLanguage.FollowSystem] already resolved. */
    var current by mutableStateOf(AppLanguage.SimplifiedChinese)
        private set

    private var packs: Map<AppLanguage, Map<String, String>> = emptyMap()

    /** Loads the packaged language files. Called once from the Application. */
    fun attach(context: Context) {
        if (packs.isNotEmpty()) return
        val app = context.applicationContext
        packs = AppLanguage.entries
            .filter { it.pack.isNotEmpty() }
            .associateWith { language -> loadPack(app, language.pack) }
    }

    private fun loadPack(context: Context, code: String): Map<String, String> = runCatching {
        val text = context.assets.open("i18n/$code.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json.parseToJsonElement(text).jsonObject.mapValues { it.value.jsonPrimitive.content }
    }.getOrDefault(emptyMap())

    /** Applies a stored choice on start-up and whenever the user picks a new one. */
    fun set(value: AppLanguage) {
        choice = value
        current = resolve(value)
    }

    /**
     * Maps [AppLanguage.FollowSystem] onto the system UI language. The activity has no
     * `configChanges` for locale, so changing the system language recreates it and [set] runs
     * again with the new locale.
     */
    fun resolve(value: AppLanguage): AppLanguage = when (value) {
        AppLanguage.FollowSystem -> systemLanguage()
        else -> value
    }

    /**
     * Picks a pack for [AppLanguage.FollowSystem]. Exact regional packs win, then a pack for the
     * same language, so adding a language only needs its enum entry and JSON file. Chinese needs
     * one explicit script check because the packs are split by region rather than by script.
     */
    private fun systemLanguage(): AppLanguage {
        val locale = Locale.getDefault()
        val language = locale.language
        val chinesePack = if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO"))
            "zh-TW" else "zh-CN"
        val preferred = if (language == "zh") chinesePack else locale.toLanguageTag()
        val available = AppLanguage.entries.filter { it.pack.isNotEmpty() }
        return available.firstOrNull { it.pack.equals(preferred, ignoreCase = true) }
            ?: available.firstOrNull { it.pack.substringBefore('-').equals(language, ignoreCase = true) }
            ?: AppLanguage.English
    }

    fun tr(key: String): String = lookup(key)

    fun tr(key: String, vararg args: Any): String =
        String.format(Locale.ROOT, lookup(key), *args)

    /**
     * Current pack → English fallback → key itself. The key fallback keeps unit tests (which have
     * no Context and therefore no packs) working without crashing.
     */
    private fun lookup(key: String): String {
        val language = current
        packs[language]?.get(key)?.let { return it }
        packs[AppLanguage.English]?.get(key)?.let { return it }
        Legacy[key]?.let { mapped -> packs[language]?.get(mapped)?.let { return it } }
        return key
    }
}

/** Localised lookup for UI copy. Identifiers are documented in `assets/i18n/en.json`. */
fun tr(key: String): String = Languages.tr(key)

/** Localised lookup for templated UI copy, using the same `%s` / `%d` placeholders as [String.format]. */
fun tr(key: String, vararg args: Any): String = Languages.tr(key, *args)

// Values written by older versions to persisted data. They are translated instead of showing raw.
private val Legacy = mapOf(
    "手动录入" to "account.source_manual",
    "小星页面识别 · 已确认" to "account.source_xiaoxing",
)
