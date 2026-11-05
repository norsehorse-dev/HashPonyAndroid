package com.hashpony.android.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat

/*
 * In-app language picker, ported from PGPonyAndroid (same languages, same
 * behaviour). AppCompat owns persistence: on Android 13+ the system stores the
 * per-app language (and the user can also change it in system Settings); on
 * Android 8 to 12 AppCompat stores it via AppLocalesMetadataHolderService.
 * AppCompatDelegate.getApplicationLocales() is the single source of truth, so
 * there is no second copy in DataStore to drift.
 *
 * Live switching without a black screen comes from the manifest:
 * MainActivity declares configChanges="locale|layoutDirection|screenLayout",
 * so a switch is an in-place configuration change and Compose recomposes with
 * the new strings. On Android 8 to 12 AppCompat applies it in place too, and
 * MainActivity.onConfigurationChanged passes it on to the view tree.
 *
 * Strings resolved outside an activity (ViewModel error messages) go through
 * [localizedContext], since on Android 8 to 12 AppCompat only localizes activities.
 */

/**
 * The languages HashPony ships. [tag] is the BCP-47 tag AppCompat and the
 * resource loader use (pt-BR lives in values-pt-rBR, zh-Hans in
 * values-b+zh+Hans). [nativeName] is shown untranslated, as every OS does.
 */
enum class SupportedLanguage(val tag: String, val nativeName: String) {
    EN("en", "English"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    FR("fr", "Français"),
    JA("ja", "日本語"),
    KO("ko", "한국어"),
    PT_BR("pt-BR", "Português (Brasil)"),
    RU("ru", "Русский"),
    TR("tr", "Türkçe"),
    UK("uk", "Українська"),
    ZH_HANS("zh-Hans", "简体中文");

    companion object {
        /**
         * Snap a tag to the closest shipped language, or null. Regional variants
         * fold into their language (de-AT to DE, es-419 to ES, pt-PT to PT_BR).
         * Chinese resolves to Simplified unless the tag asks for Traditional
         * (zh-Hant, zh-TW, zh-HK, zh-MO), which keeps the English fallback
         * rather than showing Simplified text.
         */
        fun resolve(tag: String?): SupportedLanguage? {
            if (tag.isNullOrBlank()) return null
            val normalized = tag.replace('_', '-')
            entries.firstOrNull { it.tag.equals(normalized, ignoreCase = true) }?.let { return it }
            return when (normalized.substringBefore('-').lowercase()) {
                "en" -> EN
                "de" -> DE
                "es" -> ES
                "fr" -> FR
                "ja" -> JA
                "ko" -> KO
                "pt" -> PT_BR
                "ru" -> RU
                "tr" -> TR
                "uk" -> UK
                "zh" -> if (isTraditionalChinese(normalized)) null else ZH_HANS
                else -> null
            }
        }

        private fun isTraditionalChinese(tag: String): Boolean {
            val parts = tag.split('-').drop(1).map { it.lowercase() }
            if ("hans" in parts) return false
            if ("hant" in parts) return true
            return parts.any { it == "tw" || it == "hk" || it == "mo" }
        }
    }
}

/** The applied language, observable so the picker's check mark moves on tap. */
object LanguageState {
    val current: MutableState<String> = mutableStateOf("en")

    /**
     * Read the stored choice, or, on first launch, the device language snapped
     * to a shipped one. Deliberately does not call setApplicationLocales here:
     * that would recreate the activity before it has drawn, and the system has
     * already picked the matching resources anyway.
     */
    fun init(context: Context) {
        val stored = AppCompatDelegate.getApplicationLocales()
        val tag = (0 until stored.size()).asSequence().mapNotNull { stored.get(it)?.toLanguageTag() }.firstOrNull()
        current.value = if (tag != null) {
            (SupportedLanguage.resolve(tag) ?: SupportedLanguage.EN).tag
        } else {
            LanguageManager.detectInitialLanguage(context).tag
        }
    }
}

object LanguageManager {

    fun detectInitialLanguage(context: Context): SupportedLanguage {
        val locales = context.resources.configuration.locales
        for (i in 0 until locales.size()) {
            SupportedLanguage.resolve(locales.get(i).toLanguageTag())?.let { return it }
        }
        return SupportedLanguage.EN
    }

    /** Apply [lang]. The state updates first so the check mark doesn't wait for the switch. */
    fun setLanguage(lang: SupportedLanguage) {
        LanguageState.current.value = lang.tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
    }

    fun current(): SupportedLanguage =
        SupportedLanguage.resolve(LanguageState.current.value) ?: SupportedLanguage.EN

    /**
     * [context] in the language chosen in the picker, for strings resolved
     * outside an activity. Android 13+ already applies the per-app language to
     * the application context; on 8 to 12 AppCompat applies it only to
     * activities, so it is applied here. Same approach as PGPony's ErrorText.
     */
    fun localizedContext(context: Context): Context {
        val chosen = AppCompatDelegate.getApplicationLocales()
        if (chosen.isEmpty) return context
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()))
        return context.createConfigurationContext(config)
    }
}
