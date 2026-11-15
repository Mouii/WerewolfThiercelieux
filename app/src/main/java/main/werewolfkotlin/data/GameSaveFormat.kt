package main.werewolfkotlin.data

import kotlinx.serialization.Serializable

/***
 * What a saved game holds, field by field : this file is the format of the save.
 *
 * The roles are not written in full : only what changed during the game. The description of a role
 * is read again from the roles file when the game is put back (see GameSnapshot).
 * A player, a role or a seat is pointed at by its place in its own list ("index"), never by its name.
 */
@Serializable
class SavedGame(
    val phase: String,
    val turn: Int,
    val inProgress: Boolean,
    val showDeaths: Boolean,
    val wolfTurnForced: Boolean,
    val fatherCanInfectSolo: Boolean,
    val eldersPowerRemoved: Boolean,
    val statusText: String,

    val roles: List<SavedRole>,
    val seats: List<SavedSeat>,
    val blocks: List<SavedBlock>,
    val history: List<SavedHistoryLine>,

    //The night in progress
    val stepPosition: Int,
    val nightSteps: List<Int>,
    val nightActive: Map<Int, List<Int>> = emptyMap(),   //moment of the night -> index of the roles called
    val calledSteps: List<Int> = emptyList(),
    val stepCalls: Map<Int, SavedNightCall> = emptyMap(), //what was said at each moment already called
    val pendingNocturnal: Map<Int, Boolean> = emptyMap(), //index of the role -> awake when the night ends
    val nightSnapshot: Map<Int, Boolean> = emptyMap(),    //index of the role -> awake when the night started
    val activeNow: List<Int> = emptyList()
)

/***
 * A role given to a player, and what the game changed on it
 */
@Serializable
class SavedRole(
    val className: String,
    val mode: String,
    val order: Int,
    val isNocturnal: Boolean,
    val powerState: String,
    val isWerewolf: Boolean,
    val isInfected: Boolean,
    val doTargeting: Boolean,
    val isAlive: Boolean,
    //Consumable power : whether the role has one, and whether it was used
    val hasConsumablePower: Boolean = false,
    val consumablePowerUsed: Boolean = false
)

/***
 * A place around the table : its player, and the role he holds (index in the roles, -1 : no role)
 */
@Serializable
class SavedSeat(
    val name: String,
    val isAlive: Boolean,
    val note: String = "",
    val roleIndex: Int = -1
)

/***
 * A block of the "Roles" tab : its title, what it is for, and the seats it holds
 */
@Serializable
class SavedBlock(
    val title: String,
    val kind: String,
    val lights: Boolean,
    //Role that owns the block : its name, its mode and the name of the role (see model.kindOf)
    val roleKey: List<String>? = null,
    val seatIndexes: List<Int> = emptyList()
)

/***
 * A line of the historic : the message of the rules and the names it speaks about.
 * The sentence itself is not saved, so the historic follows the language of the phone.
 */
@Serializable
class SavedHistoryLine(
    val turn: Int,
    val phase: String,
    val message: String,
    val names: List<String> = emptyList()
)

/***
 * What was said when a moment of the night was called
 */
@Serializable
class SavedNightCall(
    val roles: List<SavedCalledRole> = emptyList(),
    val infectedPlayers: List<String> = emptyList()
)

@Serializable
class SavedCalledRole(val roleName: String, val action: String)
