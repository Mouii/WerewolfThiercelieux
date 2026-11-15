package main.werewolfkotlin.game

import model.CharacterGame
import model.PowerState
import model.RoleId
import model.kindOf
import model.stickKeyOf

/***
 * Which role is in the game : the roles given to the players, their moment of the night,
 * their life and their death.
 */
object RoleRoster {

    /***
     * Number of players having a role of this class (dead players included)
     */
    fun countOfClass(className: String): Int = GameState.allCharacters.count { it.className == className }

    /***
     * Turn of the pack of werewolves : the simple werewolves bring it, otherwise it only exists
     * if the moderator asked for it in the turn order window (or if somebody is infected)
     */
    fun updateWerewolfTurn() {
        val alive = GameState.aliveCharacters
        val simpleWerewolf: CharacterGame? = alive.firstOrNull { it.className == RoleId.WEREWOLF }
        GameState.werewolfTurn = when {
            simpleWerewolf != null -> simpleWerewolf.order
            GameState.wolfTurnForced || alive.any { it.isInfected } -> RoleOrders.orderOf(RoleId.WEREWOLF)
            else -> -1
        }
    }

    /***
     * Moment of the night of a new role : the one of its class, the one of the roles it is linked to
     * (the wolves play with the werewolves), otherwise its usual place
     */
    private fun orderFor(prototype: CharacterGame): Int {
        val alive = GameState.aliveCharacters
        val sameClass = alive.firstOrNull { it.className == prototype.className }
        if (sameClass != null) return sameClass.order

        if (prototype.rolesToStick.isNotEmpty()) {
            val linked = alive.firstOrNull { prototype.rolesToStick.contains(stickKeyOf(it)) }
            if (linked != null) return linked.order
        }
        return RoleOrders.orderOf(prototype.className)
    }

    private fun sortAlive() {
        GameState.aliveCharacters.sortBy { it.order }
    }

    /***
     * Give a role to a player, or take it away (null). The role is added, or removed,
     * everywhere : turn order, information, roles screen.
     */
    fun setRole(seat: Seat, prototype: CharacterGame?) {
        val old = seat.character
        if (old != null && prototype != null && kindOf(old) == kindOf(prototype)) return

        if (old != null) {
            GameState.aliveCharacters.remove(old)
            GameState.allCharacters.remove(old)
            GameState.consumables.remove(old)
            GameState.pendingNocturnal.remove(old)
            GameState.nightSnapshot.remove(old)
            old.isInfected = false
        }
        seat.character = null

        if (prototype != null) {
            val created = prototype.copyAt(orderFor(prototype))

            GameState.aliveCharacters.add(created)
            sortAlive()
            GameState.allCharacters.add(created)
            if (created.powerState == PowerState.CONSUMABLE) GameState.consumables[created] = false

            seat.character = created
        }

        EffectBlocks.sync()
        updateWerewolfTurn()
    }

    /***
     * A player dies : the role leaves the game right away (turn order, powers that depend on it).
     * True when the game is over : nobody has a role in the game anymore.
     */
    fun kill(seat: Seat): Boolean {
        val character = seat.character
        seat.alive = false
        GameState.deathsBlock()?.players?.removeAll { it === seat }
        GameState.log(GameMessage.PLAYER_DEAD, seat.name, character?.name ?: "—")

        if (character == null) return false

        GameState.aliveCharacters.remove(character)
        GameState.consumables.remove(character)
        GameState.pendingNocturnal.remove(character)

        //Special powers that depend on this death
        PowerRules.afterDeath(character)
        updateWerewolfTurn()

        if (GameState.aliveCharacters.isEmpty() && GameState.seats.none { it.alive && it.character == null }) {
            GameState.inProgress = false
            return true
        }
        return false
    }

    /***
     * The turn changes : the purple players (block of the deaths) are eliminated.
     * True when one of these deaths ends the game.
     */
    fun applyPendingDeaths(): Boolean {
        val block = GameState.deathsBlock() ?: return false
        val dying = block.players.filter { it.alive }
        block.players.clear()

        var gameOver = false
        dying.forEach { if (kill(it)) gameOver = true }
        return gameOver
    }

    /***
     * Bring a dead player back to the game.
     * True when the game was over and is on again.
     */
    fun resurrect(seat: Seat): Boolean {
        val character = seat.character

        GameState.deathsBlock()?.players?.removeAll { it === seat }
        seat.alive = true
        if (character != null && GameState.aliveCharacters.none { it === character }) {
            GameState.aliveCharacters.add(character)
            sortAlive()
            if (character.powerState == PowerState.CONSUMABLE && !GameState.consumables.containsKey(character))
                GameState.consumables[character] = false
        }
        updateWerewolfTurn()

        //The game may have been over
        val reopened = !GameState.inProgress
        GameState.inProgress = true

        GameState.log(GameMessage.PLAYER_ALIVE, seat.name, character?.name ?: "—")
        return reopened
    }

    /***
     * Infected players join the werewolf team but keep everything else (turn order, targets)
     */
    fun infect(seat: Seat) {
        val c = seat.character ?: return
        c.isWerewolf = true
        c.isInfected = true
        GameState.log(GameMessage.PLAYER_INFECTED, seat.name, c.name)
    }

    fun disinfect(seat: Seat) {
        val c = seat.character ?: return
        c.isWerewolf = false
        c.isInfected = false
        GameState.log(GameMessage.PLAYER_DISINFECTED, seat.name, c.name)
    }
}
