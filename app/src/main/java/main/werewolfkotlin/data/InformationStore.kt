package main.werewolfkotlin.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/***
 * A free note of the moderator
 */
data class Information(
    var name: String,
    var description: String
)

/***
 * The free notes of the "Infos" tab, kept between the games
 */
object InformationStore {
    private const val KEY = "list"

    fun save(context: Context, informations: List<Information>) {
        val json = JSONArray()
        for (p in informations) {
            json.put(JSONObject().apply {
                put("name", p.name)
                put("description", p.description)
            })
        }
        context.prefs(Prefs.INFORMATION).edit().putString(KEY, json.toString()).apply()
    }

    fun load(context: Context): List<Information> {
        val json = context.prefs(Prefs.INFORMATION).getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Information(name = obj.getString("name"), description = obj.getString("description"))
        }
    }
}
