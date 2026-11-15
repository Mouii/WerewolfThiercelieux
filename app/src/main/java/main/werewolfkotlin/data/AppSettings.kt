package main.werewolfkotlin.data

import android.content.Context

/***
 * Language of the application
 */
object AppSettings {
    private const val LANGUAGE = "language"
    private const val SPINNER_POSITION = "spinner_position"

    /** Languages handled by the application. English is the default one */
    val languages: List<String> = listOf("en", "fr")

    fun language(context: Context): String? = context.prefs(Prefs.APP).getString(LANGUAGE, null)

    /** English by default */
    fun languageOrEnglish(context: Context): String = language(context) ?: "en"

    fun setLanguage(context: Context, language: String) {
        context.prefs(Prefs.APP).edit().putString(LANGUAGE, language).apply()
    }

    /** Position of the language in the spinner of the main screen, -1 when the moderator never chose */
    fun spinnerPosition(context: Context): Int = context.prefs(Prefs.APP).getInt(SPINNER_POSITION, -1)

    fun setLanguageChoice(context: Context, language: String, position: Int) {
        context.prefs(Prefs.APP).edit().putString(LANGUAGE, language).putInt(SPINNER_POSITION, position).apply()
    }
}
