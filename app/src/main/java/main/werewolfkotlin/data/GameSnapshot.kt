package main.werewolfkotlin.data

import android.content.Context
import android.util.Log
import main.werewolfkotlin.game.EffectBlock
import main.werewolfkotlin.game.EffectBlocks
import main.werewolfkotlin.game.EnumPhase
import main.werewolfkotlin.game.GameMessage
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.GameText
import main.werewolfkotlin.game.HistoryEntry
import main.werewolfkotlin.game.NightCall
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import model.CharacterGame
import model.PowerState

/***
 * The game in progress (GameState) turned into the save format (SavedGame), and put back from it.
 *
 * The roles are not written in full : the role of the roles file is found again from its name and its mode,
 * and what changed during the game (order, awake, power...) is applied on it. So a game can only be
 * resumed while its roles still exist in the roles file.
 */
object GameSnapshot {

    private const val TAG = "GameSnapshot"

    // ── Write ──────────────────────────────────────────────────────

    fun build(): SavedGame {
        val state = GameState

        //A role is pointed at by its place in the list of the roles of the game
        val indexOfRole = HashMap<CharacterGame, Int>()
        state.allCharacters.forEachIndexed { index, character -> indexOfRole[character] = index }

        val indexOfSeat = HashMap<Seat, Int>()
        state.seats.forEachIndexed { index, seat -> indexOfSeat[seat] = index }

        return SavedGame(
            phase = state.phase.name,
            turn = state.turn,
            inProgress = state.inProgress,
            showDeaths = state.showDeaths,
            wolfTurnForced = state.wolfTurnForced,
            fatherCanInfectSolo = state.fatherCanInfectSolo,
            eldersPowerRemoved = state.eldersPowerRemoved,
            statusText = state.statusText,

            roles = state.allCharacters.map { character ->
                SavedRole(
                    className = character.className,
                    mode = character.mode,
                    order = character.order,
                    isNocturnal = character.isNocturnal,
                    powerState = character.powerState.name,
                    isWerewolf = character.isWerewolf,
                    isInfected = character.isInfected,
                    doTargeting = character.doTargeting,
                    isAlive = state.aliveCharacters.any { it === character },
                    hasConsumablePower = state.consumables.containsKey(character),
                    consumablePowerUsed = state.consumables[character] ?: false
                )
            },

            seats = state.seats.map { seat ->
                SavedSeat(
                    name = seat.name,
                    isAlive = seat.alive,
                    note = seat.note,
                    roleIndex = seat.character?.let { indexOfRole[it] } ?: -1
                )
            },

            blocks = state.effectBlocks.map { block ->
                SavedBlock(
                    title = block.title,
                    kind = block.kind.name,
                    lights = block.lights,
                    roleKey = block.roleKey?.let { listOf(it.first, it.second, it.third) },
                    seatIndexes = block.players.mapNotNull { indexOfSeat[it] }
                )
            },

            history = state.history.map { line ->
                SavedHistoryLine(line.turn, line.phase.name, line.text.message.name, line.text.names)
            },

            stepPosition = state.stepPos,
            nightSteps = state.nightSteps,
            nightActive = state.nightActive.mapValues { (_, roles) -> roles.mapNotNull { indexOfRole[it] } },
            calledSteps = state.calledOrders.toList(),
            stepCalls = state.stepCalls.mapValues { (_, call) -> savedCall(call) },
            pendingNocturnal = state.pendingNocturnal.mapKeys { indexOfRole[it.key] ?: -1 },
            nightSnapshot = state.nightSnapshot.mapKeys { indexOfRole[it.key] ?: -1 },
            activeNow = state.activeNow.mapNotNull { indexOfRole[it] }
        )
    }

    private fun savedCall(call: NightCall) = SavedNightCall(
        roles = call.roles.map { SavedCalledRole(it.roleName, it.action) },
        infectedPlayers = call.infectedPlayers
    )

    // ── Read ───────────────────────────────────────────────────────

    /***
     * Put the game back as it was. False if something is wrong (a role that does not exist anymore,
     * a save of an older version...) : the game is then left empty, and a new one can be started.
     */
    fun restore(context: Context, saved: SavedGame): Boolean {
        try {
            RoleOrderStore.load(context)
            val state = GameState
            state.reset()

            //Roles : a copy of the role of the roles file, with what changed during the game
            val roles = saved.roles.map { line ->
                val prototype = RolesRepository.prototypeOf(line.className, line.mode)
                    ?: throw IllegalStateException("Unknown role " + line.className)

                val character = prototype.copyAt(line.order)
                character.doTargeting = line.doTargeting
                character.isNocturnal = line.isNocturnal
                character.powerState = PowerState.valueOf(line.powerState)
                character.isWerewolf = line.isWerewolf
                character.isInfected = line.isInfected
                character
            }
            state.allCharacters.addAll(roles)

            saved.roles.forEachIndexed { index, line ->
                val character = roles[index]
                if (line.isAlive) state.aliveCharacters.add(character)
                if (line.hasConsumablePower) state.consumables[character] = line.consumablePowerUsed
            }
            state.aliveCharacters.sortBy { it.order }

            //Players around the table
            for (line in saved.seats) {
                val seat = Seat(line.name, null, line.isAlive, line.note)
                if (line.roleIndex in roles.indices) seat.character = roles[line.roleIndex]
                state.seats.add(seat)
            }

            state.catalog = ExtensionSettings.availableRoles(context)

            //Blocks of the "Roles" tab
            for (line in saved.blocks) {
                val key = line.roleKey
                    ?.takeIf { it.size == 3 }
                    ?.let { Triple(it[0], it[1], it[2]) }

                val block = EffectBlock(line.title, EffectBlock.Kind.valueOf(line.kind), key)
                block.lights = line.lights
                for (seatIndex in line.seatIndexes) {
                    if (seatIndex in state.seats.indices) block.players.add(state.seats[seatIndex])
                }
                state.effectBlocks.add(block)
            }
            EffectBlocks.sync()

            for (line in saved.history) {
                state.history.add(
                    HistoryEntry(
                        line.turn,
                        EnumPhase.valueOf(line.phase),
                        GameText(GameMessage.valueOf(line.message), line.names)
                    )
                )
            }

            state.phase = EnumPhase.valueOf(saved.phase)
            state.turn = saved.turn
            state.inProgress = saved.inProgress
            state.showDeaths = saved.showDeaths
            state.wolfTurnForced = saved.wolfTurnForced
            state.fatherCanInfectSolo = saved.fatherCanInfectSolo
            state.eldersPowerRemoved = saved.eldersPowerRemoved
            state.statusText = saved.statusText

            //The night in progress
            state.stepPos = saved.stepPosition
            state.nightSteps = saved.nightSteps
            state.nightActive = saved.nightActive.mapValues { (_, indexes) -> indexes.map { roles[it] } }
            state.calledOrders.addAll(saved.calledSteps)
            saved.stepCalls.forEach { (order, call) -> state.stepCalls[order] = nightCall(call) }
            saved.pendingNocturnal.forEach { (index, awake) -> state.pendingNocturnal[roles[index]] = awake }
            saved.nightSnapshot.forEach { (index, awake) -> state.nightSnapshot[roles[index]] = awake }
            state.activeNow = saved.activeNow.map { roles[it] }

            RoleRoster.updateWerewolfTurn()
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "The saved game could not be read, it is dropped", ex)
            GameState.reset()
            return false
        }
    }

    private fun nightCall(saved: SavedNightCall) = NightCall(
        roles = saved.roles.map { NightCall.CalledRole(it.roleName, it.action) },
        infectedPlayers = saved.infectedPlayers
    )
}
