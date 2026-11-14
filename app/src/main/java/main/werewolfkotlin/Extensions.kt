package main.werewolfkotlin

import android.content.Context
import androidx.annotation.StringRes
import model.CharacterGame

/***
 * The extensions of the game. A role belongs to one extension, and only the roles
 * of the activated extensions can be given to the players.
 */
enum class Extension(val id: String, @StringRes val nameRes: Int) {
    MAIN("main", R.string.Extension_Main),
    NEW_MOON("new_moon", R.string.Extension_NewMoon),
    VILLAGE("village", R.string.Extension_Village),
    CHARACTERS("characters", R.string.Extension_Characters),
    YEARS_25("years_25", R.string.Extension_25Years)
}

object Extensions {
    private const val PREFS = "Extensions"

    /** Extension of each role. It is set here, inside the application. */
    private val defaults: Map<String, Extension> = mapOf(
        "Villager" to Extension.MAIN,
        "Werewolf" to Extension.MAIN,
        "Seer" to Extension.MAIN,
        "Witch" to Extension.MAIN,
        "Hunter" to Extension.MAIN,
        "Cupid" to Extension.MAIN,
        "LittleGirl" to Extension.MAIN,
        "Thief" to Extension.MAIN,
        "Defender" to Extension.MAIN,
        "Elder" to Extension.MAIN,
        "Idiot" to Extension.MAIN,
        "Scapegoat" to Extension.MAIN,

        "Actor" to Extension.NEW_MOON,
        "Angel" to Extension.NEW_MOON,
        "Crow" to Extension.NEW_MOON,
        "Gypsy" to Extension.NEW_MOON,
        "Piper" to Extension.NEW_MOON,
        "Servant" to Extension.NEW_MOON,

        "BearTamer" to Extension.VILLAGE,
        "Brother" to Extension.VILLAGE,
        "Sister" to Extension.VILLAGE,
        "Fox" to Extension.VILLAGE,
        "RustyKnight" to Extension.VILLAGE,
        "StutteringJudge" to Extension.VILLAGE,
        "VillagerVillager" to Extension.VILLAGE,

        "WildChild" to Extension.CHARACTERS,
        "WolfHound" to Extension.CHARACTERS,
        "WolfFather" to Extension.CHARACTERS,
        "BigBadWolf" to Extension.CHARACTERS,
        "WhiteWerewolf" to Extension.CHARACTERS,
        "Manipulator" to Extension.CHARACTERS,
        "Fireman" to Extension.CHARACTERS,
        "Colossus" to Extension.YEARS_25,
        "PowerfulMotherOfWolves" to Extension.YEARS_25,
        "Puppeteer" to Extension.YEARS_25,
        "TrainedMonkey" to Extension.YEARS_25
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** By default every extension is activated */
    fun isEnabled(context: Context, extension: Extension): Boolean =
        prefs(context).getBoolean("enabled_" + extension.id, true)

    fun setEnabled(context: Context, extension: Extension, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled_" + extension.id, enabled).apply()
    }

    /** Extension of a role. A role that is not in the list (created by the moderator) is in the main game */
    fun extensionOf(context: Context, className: String): Extension = defaults[className] ?: Extension.MAIN

    /** Every role (one per mode) that belongs to an activated extension */
    fun availableRoles(context: Context): List<CharacterGame> {
        return WorkerEasier.characterListType
            .flatMap { it.second.values }
            .filter { isEnabled(context, extensionOf(context, it.className)) }
            .sortedBy { it.name }
    }
}
