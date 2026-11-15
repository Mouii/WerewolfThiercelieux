package main.werewolfkotlin.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

/***
 * The game in progress is kept when the moderator leaves it, so it can be resumed from the setup page.
 * The format of what is written is in GameSaveFormat, and GameSnapshot fills it.
 */
object GameSaveStore {
    private const val TAG = "GameSaveStore"
    private const val KEY = "state"

    //ignoreUnknownKeys : a save written by an older version of the application can still be read
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    fun exists(context: Context): Boolean = context.prefs(Prefs.GAME_SAVE).contains(KEY)

    /***
     * The game that was left, or null if there is none (or if it cannot be read anymore)
     */
    fun read(context: Context): SavedGame? {
        val json = context.prefs(Prefs.GAME_SAVE).getString(KEY, null) ?: return null
        return try {
            jsonFormat.decodeFromString<SavedGame>(json)
        } catch (ex: Exception) {
            Log.e(TAG, "The saved game cannot be read, it is dropped", ex)
            clear(context)
            null
        }
    }

    fun write(context: Context, game: SavedGame) {
        val json = jsonFormat.encodeToString(game)
        context.prefs(Prefs.GAME_SAVE).edit().putString(KEY, json).apply()
    }

    fun clear(context: Context) {
        context.prefs(Prefs.GAME_SAVE).edit().remove(KEY).apply()
    }
}
