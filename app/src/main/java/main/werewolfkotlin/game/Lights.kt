package main.werewolfkotlin.game

import model.CharacterGame
import model.kindOf

/***
 * Lights of the called roles : red for the players they chose, green for the players who chose them.
 * Only the roles whose option is enabled in the information window count.
 */
object Lights {

    /** The red players (targets) and the green players (selectors) */
    class Result(val targets: Set<Seat>, val selectors: Set<Seat>)

    fun compute(focus: List<CharacterGame>): Result {
        val targets = mutableSetOf<Seat>()
        val selectors = mutableSetOf<Seat>()
        val seats = GameState.seats

        for (character in focus) {
            val key = kindOf(character)
            val seatOfRole = seats.firstOrNull { it.character === character }

            for (block in GameState.effectBlocks) {
                val owner = block.roleKey
                if (!block.lights || owner == null) continue

                //During the turn of the role : the players it chose
                if (owner == key) targets.addAll(block.players)

                //During the turn of a chosen player : the players of the role who chose it
                if (seatOfRole != null && block.players.any { it === seatOfRole }) {
                    seats.filter { other ->
                        val role = other.character
                        role != null && kindOf(role) == owner
                    }.forEach { selectors.add(it) }
                }
            }
        }
        return Result(targets, selectors)
    }
}
