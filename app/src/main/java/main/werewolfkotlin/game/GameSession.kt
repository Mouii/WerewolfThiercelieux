package main.werewolfkotlin.game

import model.CharacterGame
import model.PowerState

/***
 * The life of a whole game : start, restart, remove the roles.
 */
object GameSession {

    /***
     * Prepare the game : one seat per player, and the roles that can be given
     */
    fun start(names: List<String>, catalog: List<CharacterGame>) {
        names.forEachIndexed { i, name ->
            GameState.seats.add(Seat(name.ifBlank { GameState.labels.seatName(i + 1) }))
        }

        //Roles of the activated extensions. They join the game when they are given to a player
        GameState.catalog = catalog
        EffectBlocks.sync()
        RoleRoster.updateWerewolfTurn()
    }

    /***
     * Start the game again : same players, same roles, but nobody is dead, no target, everybody sleeps.
     * Each role comes back to its original state (powers not used, nobody infected...) and keeps its place
     * in the turn order. [prototypeOf] gives the role of the roles file from its class and the key of its variant.
     */
    fun restart(prototypeOf: (className: String, key: String) -> CharacterGame?) {
        val state = GameState
        val renewed = mutableMapOf<CharacterGame, CharacterGame>()
        for (old in state.allCharacters) {
            val prototype = prototypeOf(old.className, old.key)
            if (prototype == null) {
                old.isInfected = false
                renewed[old] = old
                continue
            }
            renewed[old] = prototype.copyAt(old.order)
        }

        val roles = state.allCharacters.map { renewed[it] ?: it }
        state.allCharacters.clear()
        state.allCharacters.addAll(roles)
        state.aliveCharacters.clear()
        state.aliveCharacters.addAll(roles)
        state.aliveCharacters.sortBy { it.order }

        //Everybody is alive again, with the same role
        state.seats.forEach { seat ->
            seat.alive = true
            seat.character = seat.character?.let { renewed[it] ?: it }
        }

        state.consumables.clear()
        roles.filter { it.powerState == PowerState.CONSUMABLE }.forEach { state.consumables[it] = false }

        //No target, no death, no historic
        EffectBlocks.clearPlayers()
        state.history.clear()
        state.pendingNocturnal.clear()
        state.nightSnapshot.clear()
        state.eldersPowerRemoved = false
        state.fatherCanInfectSolo = false

        //Nothing is called anymore, the village sleeps
        state.clearNight()
        state.inProgress = true
        state.phase = EnumPhase.SUNSET
        state.turn = 0

        RoleRoster.updateWerewolfTurn()
        state.log(GameMessage.GAME_RESTARTED)
    }

    /***
     * Remove the role of every player (the names stay)
     */
    fun resetRoles() {
        val state = GameState
        state.seats.forEach {
            it.character = null
            it.alive = true
        }
        state.aliveCharacters.clear()
        state.allCharacters.clear()
        state.consumables.clear()
        state.pendingNocturnal.clear()
        state.nightSnapshot.clear()
        state.eldersPowerRemoved = false
        EffectBlocks.clearPlayers()

        //Nothing is called anymore
        state.clearNight()
        state.inProgress = true

        EffectBlocks.sync()
        RoleRoster.updateWerewolfTurn()
        state.log(GameMessage.ROLES_RESET)
    }
}
