package main.werewolfkotlin

import android.content.Context
import org.json.JSONObject

/***
 * The game in progress is kept when the moderator leaves it, so it can be resumed from the setup page.
 */
object GameSave {
    private const val PREFS = "GameSave"
    private const val KEY = "state"

    fun exists(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY)

    fun read(context: Context): JSONObject? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return try { JSONObject(json) } catch (ex: Exception) { null }
    }

    fun write(context: Context, state: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, state.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
