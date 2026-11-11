package main.werewolfkotlin

import android.content.Context
import androidx.annotation.StringRes
import model.CharacterGame

/***
 * One line of the historic
 */
data class HistoryEntry(val turn: Int, val phase: EnumPhase, val text: String)

/***
 * Block of the "Roles" tab : a named list of players linked to the effect of a role.
 * The players can only be added by selecting them.
 */
class EffectBlock(var title: String, val kind: Kind = Kind.TARGETS) {
    enum class Kind { TARGETS, INFECTION }

    val players: MutableList<Seat> = mutableListOf()
}

/***
 * Same kind of role = same class, same mode, same name
 */
fun kindOf(c: CharacterGame) = Triple(c.className, c.mode, c.name)

/***
 * Everything shared between the activities of a game. Cleared when GameActivity is destroyed.
 */
object GameState {
    var phase: EnumPhase = EnumPhase.SUNSET
    var turn: Int = 0

    /** Every role of the game, alive or dead (characterGames only keeps the alive ones) */
    val allCharacters: MutableList<CharacterGame> = mutableListOf()

    val history: MutableList<HistoryEntry> = mutableListOf()
    val effectBlocks: MutableList<EffectBlock> = mutableListOf()

    /** Awake / asleep changes asked during the night, applied when the night ends */
    val pendingNocturnal: MutableMap<CharacterGame, Boolean> = mutableMapOf()

    /** State of every role when the night started */
    val nightSnapshot: MutableMap<CharacterGame, Boolean> = mutableMapOf()

    /** Option of the infect father of wolves : can he infect solo roles ? */
    var fatherCanInfectSolo: Boolean = false

    /** The elder power (remove the powers of the villagers) was used */
    var eldersPowerRemoved: Boolean = false

    fun reset() {
        phase = EnumPhase.SUNSET
        turn = 0
        allCharacters.clear()
        history.clear()
        effectBlocks.clear()
        pendingNocturnal.clear()
        nightSnapshot.clear()
        fatherCanInfectSolo = false
        eldersPowerRemoved = false
    }

    /***
     * Add a line to the historic, with the turn and the phase of now
     */
    fun log(context: Context, @StringRes res: Int, vararg args: Any) {
        history.add(HistoryEntry(turn, phase, context.getString(res, *args)))
    }

    /***
     * Is the role awake at night ? During the night, a change waiting for the end counts.
     */
    fun shownAwake(c: CharacterGame): Boolean {
        pendingNocturnal[c]?.let { return it }
        return if (phase == EnumPhase.NIGHT) nightSnapshot[c] ?: c.isNocturnal else c.isNocturnal
    }

    /***
     * Wake up / put to sleep some roles at once (one line in the historic).
     * During the night, the change is only applied when the night ends.
     */
    fun setAwakeGroup(context: Context, members: List<CharacterGame>, awake: Boolean, label: String) {
        var changed = false
        for (c in members) {
            if (phase == EnumPhase.NIGHT) {
                val base = nightSnapshot[c] ?: c.isNocturnal
                val shown = pendingNocturnal[c] ?: base
                if (shown == awake) continue
                if (awake == base) pendingNocturnal.remove(c) else pendingNocturnal[c] = awake
                changed = true
            } else if (c.isNocturnal != awake) {
                c.isNocturnal = awake
                changed = true
            }
        }
        if (!changed) return

        val res = if (phase == EnumPhase.NIGHT) {
            if (awake) R.string.History_AwakePending else R.string.History_AsleepPending
        } else {
            if (awake) R.string.History_Awake else R.string.History_Asleep
        }
        log(context, res, label)
    }

    fun applyPending() {
        pendingNocturnal.forEach { (character, awake) -> character.isNocturnal = awake }
        pendingNocturnal.clear()
    }

    /***
     * Infected players join the werewolf team but keep everything else (turn order, targets)
     */
    fun infect(context: Context, seat: Seat) {
        val c = seat.character ?: return
        c.isWerewolf = true
        c.infected = true
        log(context, R.string.History_Infected, seat.name, c.name)
    }

    fun disinfect(context: Context, seat: Seat) {
        val c = seat.character ?: return
        c.isWerewolf = false
        c.infected = false
        log(context, R.string.History_Disinfected, seat.name, c.name)
    }
}
