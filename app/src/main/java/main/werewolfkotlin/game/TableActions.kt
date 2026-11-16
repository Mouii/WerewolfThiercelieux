package main.werewolfkotlin.game

import model.CharacterGame
import model.kindOf

/***
 * What the moderator does on the players : choose a victim, a target, give the roles at random
 */
object TableActions {

    /***
     * Victim of a called role that kills : it goes in the block of the deaths (same player again : it comes out).
     * Returns the message to show, null if no called role kills (see ui.Narrator for the text).
     */
    fun toggleKill(seat: Seat): GameText? {
        val killerName = GameState.activeNow.firstOrNull { it.doKilling }?.name
            ?: packWithoutWolvesName()
            ?: return null
        val block = GameState.deathsBlock() ?: return null

        val message = if (block.players.removeAll { it === seat }) {
            GameMessage.DEATH_TARGET_REMOVED
        } else {
            block.players.add(seat)
            GameMessage.DEATH_TARGET
        }
        GameState.log(message, killerName, seat.name)
        return GameText(message, listOf(killerName, seat.name))
    }

    /***
     * Name of the pack when it is called without any werewolf role in the game, null otherwise
     */
    private fun packWithoutWolvesName(): String? {
        if (GameState.phase != EnumPhase.NIGHT) return null
        val called = GameState.nightSteps.getOrNull(GameState.stepPos) ?: return null
        return if (NightFlow.isPackWithoutWolves(called)) GameState.labels.packName else null
    }

    /***
     * Target of the called role, stored in the block of the role (same player again : it is removed).
     * Returns the message to show, null if no called role has a block of targets.
     */
    fun toggleTarget(seat: Seat): GameText? {
        //A living player is chosen by a role that chooses the living, a dead one by a role
        //that chooses the dead (the necromancer)
        val block = GameState.activeNow
            .filter { it.targetsDead != seat.alive }
            .firstNotNullOfOrNull { c -> GameState.effectBlocks.firstOrNull { it.kind == EffectBlock.Kind.TARGETS && it.roleKey == kindOf(c) } }
            ?: return null

        val message = if (block.players.any { it === seat }) {
            block.players.removeAll { it === seat }
            GameMessage.TARGET_REMOVED
        } else {
            EffectBlocks.addPlayer(block, seat)
            GameMessage.TARGET
        }
        GameState.log(message, block.title, seat.name)
        return GameText(message, listOf(block.title, seat.name))
    }

    /***
     * Purple : the player is in the block of the deaths, he dies when the turn changes
     */
    fun toggleDeath(seat: Seat) {
        val block = GameState.deathsBlock() ?: return
        if (!block.players.removeAll { it === seat }) block.players.add(seat)
    }

    /** The players who are alive and have no role */
    fun roleless(): List<Seat> = GameState.seats.filter { it.alive && it.character == null }

    /***
     * The chosen roles are given at random to the players without role (and to the replaced ones).
     * If there are less roles than players, the players left have no role.
     */
    fun giveRolesAtRandom(chosen: List<CharacterGame>, replaced: List<Seat>) {
        replaced.forEach { RoleRoster.setRole(it, null) }

        val players = roleless().shuffled()
        val roles = chosen.shuffled()
        for (i in 0 until minOf(players.size, roles.size)) {
            RoleRoster.setRole(players[i], roles[i])
        }
    }
}
