package main.werewolfkotlin

import android.content.Context
import org.json.JSONObject

/***
 * Moment of the night of each role. The roles are added to the game when they are given to a player,
 * so each role has a place in the turn order from the start. The moderator can move the roles
 * in the turn order window : the new places are kept for the next games.
 */
object RoleOrder {
    private const val PREFS = "RoleOrder"
    private const val KEY = "orders"

    /** Usual order of the night. The day roles come last */
    private val defaultSequence = listOf(
        "Thief", "Actor", "Cupid", "WildChild", "Brother", "Sister", "Gypsy", "Manipulator",
        "Defender", "Fox", "Seer", "TrainedMonkey", "PowerfulMotherOfWolves",
        "Werewolf", "WolfFather", "WolfHound", "LittleGirl", "BigBadWolf", "WhiteWerewolf",
        "Witch", "Piper", "Crow",
        "Angel", "BearTamer", "Colossus", "Elder", "Fireman", "Hunter", "Idiot", "Puppeteer",
        "RustyKnight", "Scapegoat", "Servant", "StutteringJudge", "Villager", "VillagerVillager"
    )

    private val saved: MutableMap<String, Int> = mutableMapOf()

    fun load(context: Context) {
        saved.clear()
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { saved[it] = obj.getInt(it) }
        } catch (ex: Exception) {
            saved.clear()
        }
    }

    /** Places are spaced by 10, so two roles never share a moment unless they are linked */
    private fun defaultOf(className: String): Int {
        val index = defaultSequence.indexOf(className)
        return if (index >= 0) (index + 1) * 10 else (defaultSequence.size + 1) * 10 + (className.hashCode() and 7)
    }

    fun orderOf(className: String): Int = saved[className] ?: defaultOf(className)

    fun set(context: Context, className: String, order: Int) {
        saved[className] = order
        val obj = JSONObject()
        saved.forEach { (name, value) -> obj.put(name, value) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, obj.toString()).apply()
    }
}
