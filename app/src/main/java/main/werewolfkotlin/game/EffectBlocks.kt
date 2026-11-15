package main.werewolfkotlin.game

import model.PowerState
import model.RoleId
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

        blocks.removeAll { it.roleKey != null && it.roleKey !in wanted }

        for (character in kinds) {
            if (blocks.none { it.roleKey == kindOf(character) }) {
                blocks.add(
                    if (character.className == RoleId.WOLF_FATHER)
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
     * A new night starts : the roles with a permanent power forget their targets.
     * The roles with a unique power (used once in the game) keep them.
     */
    fun resetPermanentTargets() {
        for (block in GameState.effectBlocks) {
            if (block.kind != EffectBlock.Kind.TARGETS) continue
            val key = block.roleKey ?: continue
            val role = GameState.allCharacters.firstOrNull { kindOf(it) == key } ?: continue
            if (role.powerState == PowerState.PERMANENT) block.players.clear()
        }
    }

    /** Nobody is targeted, nobody is going to die */
    fun clearPlayers() {
        GameState.effectBlocks.forEach { it.players.clear() }
    }
}
