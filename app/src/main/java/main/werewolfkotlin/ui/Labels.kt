package main.werewolfkotlin.ui

import android.content.Context
import main.werewolfkotlin.data.stringByKey
import main.werewolfkotlin.game.EnumPhase
import main.werewolfkotlin.R
import model.ConditionalActivation
import model.Extension
import model.PowerState

/***
 * Text shown for the values of the model (phase, power, condition, extension), in the language of the application.
 * Looked for once, then kept : clear() when the language changes.
 */
object Labels {
    private val cache: MutableMap<String, String> = mutableMapOf()

    private fun get(context: Context, key: String): String = cache.getOrPut(key) { context.stringByKey(key) }

    fun clear() = cache.clear()

    fun phase(context: Context, phase: EnumPhase): String = get(context, "Phase_$phase")

    fun power(context: Context, power: PowerState): String = get(context, "Power_$power")

    fun condition(context: Context, condition: ConditionalActivation): String = get(context, "Condition_$condition")

    fun extension(context: Context, extension: Extension): String = context.getString(
        when (extension) {
            Extension.MAIN -> R.string.Extension_Main
            Extension.NEW_MOON -> R.string.Extension_NewMoon
            Extension.VILLAGE -> R.string.Extension_Village
            Extension.CHARACTERS -> R.string.Extension_Characters
            Extension.YEARS_25 -> R.string.Extension_25Years
            Extension.CUSTOMIZE -> R.string.Extension_Custom
        }
    )
}
