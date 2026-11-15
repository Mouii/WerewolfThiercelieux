package main.werewolfkotlin.ui

import android.content.Context
import main.werewolfkotlin.R
import main.werewolfkotlin.game.EnumPhase
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.NightFlow
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.view.BandEntry

/***
 * The turn order as the screen shows it : one entry per moment of the night
 */
object BandEntries {

    /***
     * Entries of the band : one per moment of the turn order
     */
    fun build(context: Context): List<BandEntry> {
        val state = GameState
        val orders = (state.aliveCharacters.map { it.order } + state.nightActive.keys +
                (if (state.werewolfTurn >= 0) listOf(state.werewolfTurn) else emptyList())).toSortedSet()

        return orders.map { order ->
            val members = NightFlow.membersOf(order)
            val awake = if (state.phase == EnumPhase.NIGHT) order in state.nightSteps else NightFlow.isAwakeNow(order)
            val pending = members.mapNotNull { state.pendingNocturnal[it] }.firstOrNull()

            val label = members.groupBy { it.name }
                .map { (name, list) -> if (list.size > 1) "$name (${list.size})" else name }
                .joinToString(" + ")
                .ifEmpty { if (order == state.werewolfTurn) context.getString(R.string.WolfTurn_Name) else "" }

            val icon = if (members.isNotEmpty()) RoleImages.of(members.first())
                       else if (order == state.werewolfTurn) RoleImages.WOLF_PACK else 0

            BandEntry(order, label, icon, awake, if (pending != null && pending != awake) pending else null)
        }
    }

    /***
     * Awake / asleep switch of a role.
     * During the night the change waits for the end of the night, otherwise it is immediate.
     */
    fun toggleAwake(context: Context, order: Int) {
        val members = NightFlow.membersOf(order)
        val entry = build(context).firstOrNull { it.order == order }
        if (members.isEmpty() || entry == null) return

        val newState = !(entry.pendingAwake ?: entry.awake)
        NightFlow.setAwakeGroup(members, newState, entry.label)
    }
}
