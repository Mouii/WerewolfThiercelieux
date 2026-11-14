package main.werewolfkotlin

import android.content.Context
import androidx.annotation.StringRes
import model.CharacterGame
import model.PowerState
import model.WolfFather

/***
 * One line of the historic
 */
data class HistoryEntry(val turn: Int, val phase: EnumPhase, val text: String)

/***
 * Block of the "Roles" tab : a named list of players linked to the effect of a role.
 * The players can only be added by selecting them.
 * - TARGETS : the players chosen by the role
 * - INFECTION : the players infected by the infect father of wolves
 * - DEATHS : the dead players, with a button to clear them quickly
 */
class EffectBlock(
    var title: String,
    val kind: Kind = Kind.TARGETS,
    val roleKey: Triple<String, String, String>? = null   //Role who owns the block, see kindOf()
) {
    enum class Kind { TARGETS, INFECTION, DEATHS }

    val players: MutableList<Seat> = mutableListOf()

    /** Red light on the targets during the turn of the role, green light on the role during their turn */
    var lights: Boolean = kind == Kind.TARGETS && roleKey != null
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

    /** Every role given to a player, alive or dead (characterGames only keeps the alive ones) */
    val allCharacters: MutableList<CharacterGame> = mutableListOf()

    /** The roles of the activated extensions : what can be given to a player */
    var catalog: List<CharacterGame> = emptyList()

    /** The pack of werewolves is called even without simple werewolf (set in the turn order window) */
    var wolfTurnForced: Boolean = false

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
        catalog = emptyList()
        wolfTurnForced = false
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

    /***
     * Blocks of the "Roles" tab follow the roles given to the players : a block appears
     * with the first player having a role that chooses players, and disappears with the last one.
     */
    fun syncBlocks(context: Context) {
        val kinds = allCharacters.filter { it.doTargeting }.distinctBy { kindOf(it) }.sortedBy { it.order }
        val wanted = kinds.map { kindOf(it) }.toSet()

        effectBlocks.removeAll { it.roleKey != null && it.roleKey !in wanted }

        for (character in kinds) {
            if (effectBlocks.none { it.roleKey == kindOf(character) }) {
                effectBlocks.add(
                    if (character is WolfFather)
                        EffectBlock(context.getString(R.string.Block_Infected), EffectBlock.Kind.INFECTION, kindOf(character))
                    else
                        EffectBlock(character.name, EffectBlock.Kind.TARGETS, kindOf(character))
                )
            }
        }

        if (effectBlocks.none { it.kind == EffectBlock.Kind.DEATHS }) {
            effectBlocks.add(0, EffectBlock(context.getString(R.string.Block_Deaths), EffectBlock.Kind.DEATHS))
        }
    }

    /***
     * A new night starts : the roles with a permanent power forget their targets.
     * The roles with a unique power (used once in the game) keep them.
     */
    fun resetPermanentTargets() {
        for (block in effectBlocks) {
            if (block.kind != EffectBlock.Kind.TARGETS) continue
            val key = block.roleKey ?: continue
            val role = allCharacters.firstOrNull { kindOf(it) == key } ?: continue
            if (role.powerState == PowerState.PERMANENT) block.players.clear()
        }
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
        c.isInfected = true
        log(context, R.string.History_Infected, seat.name, c.name)
    }

    fun disinfect(context: Context, seat: Seat) {
        val c = seat.character ?: return
        c.isWerewolf = false
        c.isInfected = false
        log(context, R.string.History_Disinfected, seat.name, c.name)
    }
}
