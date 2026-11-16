package main.werewolfkotlin.game

import model.CharacterGame
import model.PowerState
import model.kindOf

/***
 * Rules of the blocks of the "Roles" tab
 */
object EffectBlocks {

    /***
     * Blocks follow the roles given to the players : a block appears
     * with the first player having a role that chooses players, and disappears with the last one.
     */
    fun sync() {
        val blocks = GameState.effectBlocks
        val kinds = GameState.allCharacters.filter { it.doTargeting }.distinctBy { kindOf(it) }.sortedBy { it.order }
        val wanted = kinds.map { kindOf(it) }.toSet()

        //The block of the copied player lives as long as the role that copied him is in the game
        val copiers = GameState.allCharacters.filter { it.copiedFrom != null }.map { kindOf(it) }.toSet()

        blocks.removeAll { block ->
            val owner = block.roleKey
            when {
                owner == null -> false
                block.kind == EffectBlock.Kind.MODEL -> owner !in copiers
                else -> owner !in wanted
            }
        }

        for (character in kinds) {
            if (blocks.none { it.kind != EffectBlock.Kind.MODEL && it.roleKey == kindOf(character) }) {
                blocks.add(
                    if (character.ownsInfectionBlock)
                        EffectBlock(GameState.labels.infectedBlock, EffectBlock.Kind.INFECTION, kindOf(character))
                    else
                        EffectBlock(character.name, EffectBlock.Kind.TARGETS, kindOf(character))
                )
            }
        }

        if (blocks.none { it.kind == EffectBlock.Kind.DEATHS }) {
            blocks.add(0, EffectBlock(GameState.labels.deathsBlock, EffectBlock.Kind.DEATHS))
        }
    }

    /***
     * A new night starts : the roles with a permanent power forget their targets, unless the role
     * says it keeps them. The roles with a power used once in the game always keep them.
     */
    fun resetPermanentTargets() {
        for (block in GameState.effectBlocks) {
            if (block.kind != EffectBlock.Kind.TARGETS) continue
            val key = block.roleKey ?: continue
            val role = GameState.allCharacters.firstOrNull { kindOf(it) == key } ?: continue
            if (role.powerState == PowerState.PERMANENT && !role.keepsTargets) block.players.clear()
        }
    }

    /***
     * A player is chosen by the role of a block. The roles that wait for it react (see PowerRules).
     */
    fun addPlayer(block: EffectBlock, seat: Seat) {
        if (block.players.any { it === seat }) return
        block.players.add(seat)

        if (block.kind == EffectBlock.Kind.TARGETS) PowerRules.afterTargeting(block, seat)
    }

    /***
     * A role copied the role of a player : the player leaves the targets of the role and goes in a
     * block of his own, which stays for the whole game. So who was copied is always visible,
     * whatever the copied role does with its own targets afterwards.
     */
    fun keepModel(role: CharacterGame, modelSeat: Seat, title: String) {
        val key = kindOf(role)
        GameState.effectBlocks
            .filter { it.kind == EffectBlock.Kind.TARGETS && it.roleKey == key }
            .forEach { block -> block.players.removeAll { it === modelSeat } }

        val block = EffectBlock(title, EffectBlock.Kind.MODEL, key)
        block.players.add(modelSeat)
        GameState.effectBlocks.add(block)
    }

    /** Nobody is targeted, nobody is going to die (the copied players stay : the copy is not undone) */
    fun clearPlayers() {
        GameState.effectBlocks.filter { it.kind != EffectBlock.Kind.MODEL }.forEach { it.players.clear() }
    }
}
