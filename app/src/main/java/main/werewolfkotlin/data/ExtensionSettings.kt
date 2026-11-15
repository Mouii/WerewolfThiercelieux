package main.werewolfkotlin.data

import android.content.Context
import model.CharacterGame
import model.Extension

/***
 * Which extensions are activated. Only the roles of the activated extensions can be given to the players.
 */
object ExtensionSettings {

    /** By default every extension is activated */
    fun isEnabled(context: Context, extension: Extension): Boolean =
        context.prefs(Prefs.EXTENSIONS).getBoolean("enabled_" + extension.id, true)

    fun setEnabled(context: Context, extension: Extension, enabled: Boolean) {
        context.prefs(Prefs.EXTENSIONS).edit().putBoolean("enabled_" + extension.id, enabled).apply()
    }

    fun hasEnabled(context: Context): Boolean = Extension.values().any { isEnabled(context, it) }

    /** Every role (one per mode) that belongs to an activated extension */
    fun availableRoles(context: Context): List<CharacterGame> {
        return RolesRepository.roleTypes
            .filter { isEnabled(context, Extension.of(it.first)) }
            .flatMap { it.second.values }
            .sortedBy { it.name }
    }
}
