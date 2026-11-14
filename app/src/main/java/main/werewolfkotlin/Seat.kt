package main.werewolfkotlin

import android.content.Context
import model.CharacterGame
import org.json.JSONArray

/***
 * One drawn player around the table. The role stays null until the moderator assigns it.
 */
class Seat(
    var name: String,
    var character: CharacterGame? = null,
    var alive: Boolean = true,
    var note: String = ""
)

/***
 * Seats of the current game. Like the other game globals, it is cleared when GameActivity is destroyed.
 */
val seats: MutableList<Seat> = mutableListOf()

/***
 * The names of the players are kept between the games. Only the names chosen by the moderator are saved,
 * the empty ones are the default "Player N".
 */
object PlayerNames {
    private const val PREFS = "PlayerNames"
    private const val KEY = "names"

    private fun read(context: Context): MutableList<String> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val names = mutableListOf<String>()
        if (json != null) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) names.add(array.getString(i))
            } catch (ex: Exception) {
                names.clear()
            }
        }
        return names
    }

    /***
     * Give the saved names to the seats
     */
    fun applyTo(context: Context, list: List<Seat>) {
        val saved = read(context)
        list.forEachIndexed { i, seat ->
            val name = saved.getOrNull(i)
            if (!name.isNullOrBlank()) seat.name = name
        }
    }

    /***
     * Save the names of the seats. The names of the seats that don't exist in this game are kept
     */
    fun save(context: Context, list: List<Seat>) {
        val saved = read(context)
        list.forEachIndexed { i, seat ->
            while (saved.size <= i) saved.add("")
            val default = context.getString(R.string.SeatDefaultName, i + 1)
            saved[i] = if (seat.name == default) "" else seat.name
        }
        val array = JSONArray()
        saved.forEach { array.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    /***
     * Names chosen in the game setup, "" for the players without name
     */
    fun readAll(context: Context): List<String> = read(context)

    fun saveAll(context: Context, names: List<String>) {
        val array = JSONArray()
        names.forEach { array.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    /** Number of players of the last game setup */
    fun readCount(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("count", 8)

    fun saveCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("count", count).apply()
    }

    /***
     * Forget every name, the seats come back to "Player N"
     */
    fun clear(context: Context, list: List<Seat>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
        list.forEachIndexed { i, seat -> seat.name = context.getString(R.string.SeatDefaultName, i + 1) }
    }
}
