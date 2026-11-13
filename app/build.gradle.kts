import java.util.Properties
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.exists()) releaseSigningFile.inputStream().use { load(it) }
}

/**
 * The language packs are the single source of truth, but Android reads the app name and the
 * accessibility service label/description from resources before the app process starts. Generate
 * those small resource files from the packs so translated system strings stay in the same place.
 */
abstract class GenerateI18nSystemResources : DefaultTask() {
    @get:InputDirectory
    abstract val i18nDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDir.get().asFile
        output.deleteRecursively()

        fun pack(code: String): Map<*, *> =
            JsonSlurper().parse(i18nDir.get().file("$code.json").asFile) as Map<*, *>

        fun androidString(value: Any?): String {
            val text = value?.toString() ?: error("Missing language-pack value")
            return buildString {
                append('"')
                for (c in text) {
                    when (c) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '&' -> append("&amp;")
                        '<' -> append("&lt;")
                        '>' -> append("&gt;")
                        '\n' -> append("\\n")
                        else -> append(c)
                    }
                }
                append('"')
            }
        }

        fun write(folder: String, values: Map<*, *>) {
            val dir = output.resolve(folder)
            dir.mkdirs()
            dir.resolve("strings.xml").writeText(
                buildString {
                    appendLine("<resources>")
                    appendLine("    <string name=\"app_name\">${androidString(values["app.name_full"])}</string>")
                    appendLine("    <string name=\"accessibility_service_name\">${androidString(values["system.accessibility_service_name"])}</string>")
                    appendLine("    <string name=\"accessibility_description\">${androidString(values["system.accessibility_description"])}</string>")
                    appendLine("</resources>")
                },
                Charsets.UTF_8,
            )
        }

        val packs = i18nDir.get().asFile
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name }
            .orEmpty()
        check(packs.isNotEmpty()) { "No language packs found in ${i18nDir.get().asFile}" }

        for (file in packs) {
            val code = file.nameWithoutExtension
            val values = pack(code)
            for (folder in androidResourceFolders(code)) write(folder, values)
        }
    }

    /**
     * Derives the Android resource folder names for a language-pack code, so adding a pack only
     * means adding its JSON file: `ja` → `values-ja`, `pt-BR` → `values-pt-rBR`. English is the
     * default `values`, and the two Chinese packs also get their script/region aliases.
     */
    private fun androidResourceFolders(code: String): List<String> {
        if (code == "en") return listOf("values")
        val language = code.substringBefore('-').lowercase()
        val region = code.substringAfter('-', "").uppercase()
        val exact = if (region.isEmpty()) "values-$language" else "values-$language-r$region"
        return when (code) {
            "zh-CN" -> listOf("values-zh", exact)
            "zh-TW" -> listOf(exact, "values-zh-rHK")
            else -> listOf(exact)
        }
    }
}

val generateI18nSystemResources = tasks.register<GenerateI18nSystemResources>("generateI18nSystemResources") {
    i18nDir.set(layout.projectDirectory.dir("src/main/assets/i18n"))
    outputDir.set(layout.buildDirectory.dir("generated/i18nRes"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(generateI18nSystemResources) { it.outputDir }
    }
}

android {
    namespace = "com.kelsuita.starswish"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.kelsuita.starswish"
        minSdk = 26
        targetSdk = 37
        versionCode = 20
        versionName = "Dr.Ratio 1.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    signingConfigs {
        if (releaseSigningFile.exists()) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.12.1")
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    testImplementation("junit:junit:4.13.2")
}
