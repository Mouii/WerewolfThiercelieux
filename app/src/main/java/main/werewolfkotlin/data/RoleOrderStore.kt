package main.werewolfkotlin.data

import android.content.Context
import main.werewolfkotlin.game.RoleOrders
import org.json.JSONObject

/***
 * The places of the roles in the night, kept between the games (the game works on game.RoleOrders).
 */
object RoleOrderStore {
    private const val KEY = "orders"

    /** Give the saved places to the game */
    fun load(context: Context) {
        val orders = mutableMapOf<String, Int>()
        val json = context.prefs(Prefs.ROLE_ORDER).getString(KEY, null)
        if (json != null) {
            try {
                val obj = JSONObject(json)
                obj.keys().forEach { orders[it] = obj.getInt(it) }
            } catch (ex: Exception) {
                orders.clear()
            }
        }
        RoleOrders.replaceAll(orders)
    }

    /** Keep the places of the game for the next ones */
    fun save(context: Context) {
        val obj = JSONObject()
        RoleOrders.all().forEach { (name, value) -> obj.put(name, value) }
        context.prefs(Prefs.ROLE_ORDER).edit().putString(KEY, obj.toString()).apply()
    }
}
