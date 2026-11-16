package main.werewolfkotlin.ui

import android.content.Context
import main.werewolfkotlin.R
import main.werewolfkotlin.game.GameLabels
import main.werewolfkotlin.game.GameMessage
import main.werewolfkotlin.game.GameText
import main.werewolfkotlin.game.NightCall

/***
 * Turns what the rules say into the sentences the moderator reads.
 *
 * The rules of the game (package game) never build a sentence : they return a GameMessage
 * and the names it speaks about. This file is the only place that knows the texts of the
 * translation files, so a new language only needs res/values-xx/strings.xml.
 */
object Narrator {

    /***
     * Text of a message of the rules, in the language of the phone
     */
    fun say(context: Context, text: GameText): String {
        val names: Array<Any> = text.names.toTypedArray()
        return context.getString(resourceOf(text.message), *names)
    }

    /***
     * Text of a role called during the night : one block per role, then the infected players
     */
    fun say(context: Context, call: NightCall): String {
        val lines = StringBuilder()
        for (role in call.roles) {
            lines.append(role.roleName).append("\n").append(role.action).append("\n\n")
        }
        if (call.infectedPlayers.isNotEmpty()) {
            val names = call.infectedPlayers.joinToString(", ")
            lines.append(context.getString(R.string.Pack_Infected, names)).append("\n")
        }
        return lines.toString()
    }

    /***
     * The fixed labels the rules need. Given to the rules at the start of a game.
     */
    fun labels(context: Context): GameLabels {
        //The application context, never a screen : the rules keep these labels for the whole game
        val application = context.applicationContext
        return GameLabels(
            deathsBlock = application.getString(R.string.Block_Deaths),
            infectedBlock = application.getString(R.string.Block_Infected),
            packName = application.getString(R.string.WolfTurn_Name),
            packAction = application.getString(R.string.WolfTurn_Call),
            seatName = { number -> application.getString(R.string.SeatDefaultName, number) }
        )
    }

    /***
     * The text of each message of the rules. One line per message, nothing else to do here.
     */
    private fun resourceOf(message: GameMessage): Int = when (message) {
        GameMessage.PLAYER_DEAD -> R.string.History_Dead
        GameMessage.PLAYER_ALIVE -> R.string.History_Alive
        GameMessage.PLAYER_INFECTED -> R.string.History_Infected
        GameMessage.PLAYER_DISINFECTED -> R.string.History_Disinfected
        GameMessage.ELDER_POWER_USED -> R.string.History_Elder
        GameMessage.GAME_RESTARTED -> R.string.History_Restarted
        GameMessage.ROLES_RESET -> R.string.History_RolesReset
        GameMessage.ROLES_AWAKE -> R.string.History_Awake
        GameMessage.ROLES_ASLEEP -> R.string.History_Asleep
        GameMessage.ROLES_AWAKE_PENDING -> R.string.History_AwakePending
        GameMessage.ROLES_ASLEEP_PENDING -> R.string.History_AsleepPending
        GameMessage.DEATH_TARGET -> R.string.History_DeathTarget
        GameMessage.DEATH_TARGET_REMOVED -> R.string.History_DeathTargetRemoved
        GameMessage.TARGET -> R.string.History_Target
        GameMessage.TARGET_REMOVED -> R.string.History_TargetRemoved
        GameMessage.ROLE_COPIED -> R.string.History_Copied
    }
}
