package main.werewolfkotlin.game

import model.RoleSequence

/***
 * Moment of the night of each role, in memory. The roles are added to the game when they are given to a player,
 * so each role has a place in the turn order from the start. The moderator can move the roles
 * in the turn order window : the new places are kept for the next games (see data.RoleOrderStore).
 */
object RoleOrders {
    private val saved: MutableMap<String, Int> = mutableMapOf()

    fun replaceAll(orders: Map<String, Int>) {
        saved.clear()
        saved.putAll(orders)
    }

    fun all(): Map<String, Int> = saved.toMap()

    fun orderOf(className: String): Int = saved[className] ?: RoleSequence.defaultOf(className)

    fun set(className: String, order: Int) {
        saved[className] = order
    }
}
