package main.werewolfkotlin.game

import model.CharacterGame
import model.NightSlot
import model.PowerState
import model.RoleId
import model.kindOf

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
     * Turn of the pack of werewolves : the simple werewolves bring it. Without them it still exists,
     * at its usual place, unless the moderator turned it off in the turn order window
     * (an infected player always brings it back)
     */
    fun updateWerewolfTurn() {
        val alive = GameState.aliveCharacters

        //The role that carries the pack (the simple werewolf) gives it its moment
        val packRole: CharacterGame? = alive.firstOrNull { it.definesPackTurn }
        GameState.werewolfTurn = when {
            packRole != null -> packRole.order
            GameState.wolfTurnForced || alive.any { it.isInfected } -> RoleOrders.orderOf(RoleId.WEREWOLF)
            else -> -1
        }

        //Every role that plays with the pack follows it, whatever the order the roles were given in
        if (GameState.werewolfTurn >= 0) {
            alive.filter { it.playsAt == NightSlot.WEREWOLF_TURN }
                .forEach { it.order = GameState.werewolfTurn }
        }
        sortAlive()
    }

    /***
     * Put a role on the moment of the pack, if that is where it plays
     */
    fun placeWithPack(role: CharacterGame) {
        if (role.playsAt != NightSlot.WEREWOLF_TURN) return
        if (GameState.werewolfTurn < 0) return
        role.order = GameState.werewolfTurn
        sortAlive()
    }

    /***
     * Moment of the night of a new role : the moment of the pack when it plays with it,
     * the moment of a role of the same kind already in the game, otherwise its usual place.
     */
    private fun orderFor(prototype: CharacterGame): Int {
        val alive = GameState.aliveCharacters

        if (prototype.playsAt == NightSlot.WEREWOLF_TURN && GameState.werewolfTurn >= 0)
            return GameState.werewolfTurn

        val sameClass = alive.firstOrNull { it.className == prototype.className }
        if (sameClass != null) return sameClass.order

        return freePlaceFrom(RoleOrders.orderOf(prototype.className), prototype)
    }

    /***
     * The usual place of a role, moved just after the places already taken by another role.
     * Two roles only share a moment when they are the same role, or when they play with the pack.
     */
    private fun freePlaceFrom(wanted: Int, prototype: CharacterGame): Int {
        var place = wanted
        while (GameState.aliveCharacters.any {
                it.order == place
                        && it.className != prototype.className
                        && it.playsAt == NightSlot.OWN_MOMENT
            }) place++
        return place
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

        if (character != null) {
            GameState.aliveCharacters.remove(character)
            GameState.consumables.remove(character)
            GameState.pendingNocturnal.remove(character)
        }

        //Special powers that depend on this death. A player without a role counts too :
        //a role may have chosen him (the mentor of the wild child)
        PowerRules.afterDeath(character, seat)
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
