package main.werewolfkotlin.ui.dialog

import main.werewolfkotlin.ui.design.colorOf

import android.app.Activity
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import main.werewolfkotlin.R
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.ui.adapter.TurnOrderAdapter
import main.werewolfkotlin.ui.design.dp
import main.werewolfkotlin.ui.view.BandEntry
import model.RoleId

/***
 * Window of the turn order : move the roles (drag'n'drop or arrows), wake them up or put them to sleep.
 * - [entries] gives the fresh entries of the turn order
 * - [onSwap] swaps two moments of the turn order
 * - [onToggleAwake] wakes up / puts to sleep the role of a moment
 * - [onWolfTurnChanged] is called when the moderator calls (or not) the pack of werewolves
 */
object TurnOrderDialog {

    fun show(
        activity: Activity,
        entries: () -> List<BandEntry>,
        onSwap: (Int, Int) -> Boolean,
        onToggleAwake: (Int) -> Unit,
        onWolfTurnChanged: () -> Unit
    ) {
        val panel = PanelDialog(activity, activity.getString(R.string.Band_OrderTitle), scrollable = false)

        val recycler = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.5).toInt()
            )
        }

        val adapter = TurnOrderAdapter(entries().toMutableList(), onSwap, onToggleAwake, entries)
        recycler.adapter = adapter

        val touchHelper = ItemTouchHelper(adapter.dragCallback)
        touchHelper.attachToRecyclerView(recycler)
        adapter.touchHelper = touchHelper

        //Without simple werewolf, the moderator decides if the pack of werewolves is called
        if (GameState.aliveCharacters.none { it.className == RoleId.WEREWOLF }) {
            val wolfSwitch = SwitchCompat(activity).apply {
                text = activity.getString(R.string.WolfTurn_Switch)
                setTextColor(activity.colorOf(R.color.white))
                isChecked = GameState.wolfTurnForced
                setOnCheckedChangeListener { _, checked ->
                    GameState.wolfTurnForced = checked
                    onWolfTurnChanged()
                    adapter.refresh()
                }
            }
            panel.body.addView(wolfSwitch, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(8) })
        }

        panel.body.addView(recycler)
        panel.addButton(activity.getString(R.string.Generic_Okay), accent = true)
        panel.show()
    }
}
