package main.werewolfkotlin.data

import android.content.Context
import main.werewolfkotlin.R
import main.werewolfkotlin.game.Seat
import org.json.JSONArray

/***
 * The names of the players are kept between the games. Only the names chosen by the moderator are saved,
 * the empty ones are the default "Player N".
 */
object PlayerNamesStore {
    private const val KEY = "names"
    private const val COUNT = "count"

    private fun write(context: Context, names: List<String>) {
        val array = JSONArray()
        names.forEach { array.put(it) }
        context.prefs(Prefs.PLAYER_NAMES).edit().putString(KEY, array.toString()).apply()
    }

    /***
     * Names chosen in the game setup, "" for the players without name
     */
    fun readAll(context: Context): List<String> {
        val json = context.prefs(Prefs.PLAYER_NAMES).getString(KEY, null) ?: return emptyList()
        val names = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) names.add(array.getString(i))
        } catch (ex: Exception) {
            names.clear()
        }
        return names
    }

    fun saveAll(context: Context, names: List<String>) = write(context, names)

    /***
     * Save the names of the seats. The names of the seats that don't exist in this game are kept
     */
    fun save(context: Context, list: List<Seat>) {
        val saved = readAll(context).toMutableList()
        list.forEachIndexed { i, seat ->
            while (saved.size <= i) saved.add("")
            val default = context.getString(R.string.SeatDefaultName, i + 1)
            saved[i] = if (seat.name == default) "" else seat.name
        }
        write(context, saved)
    }

    /***
     * Keep the names written in the game setup, the first ones of the list.
     * The names of the players that don't exist in this game are kept too
     */
    fun merge(context: Context, names: List<String>) {
        val saved = readAll(context).toMutableList()
        names.forEachIndexed { i, name ->
            while (saved.size <= i) saved.add("")
            saved[i] = name
        }
        write(context, saved)
    }

    /** Number of players of the last game setup */
    fun readCount(context: Context): Int = context.prefs(Prefs.PLAYER_NAMES).getInt(COUNT, 8)

    fun saveCount(context: Context, count: Int) {
        context.prefs(Prefs.PLAYER_NAMES).edit().putInt(COUNT, count).apply()
    }
}
