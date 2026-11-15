package main.werewolfkotlin.ui.dialog

import main.werewolfkotlin.ui.design.colorOf

import android.app.Activity
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import main.werewolfkotlin.R
import main.werewolfkotlin.game.NightFlow
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.dp
import model.kindOf

/***
 * Picture and description (not the action) of the roles of a moment of the turn order
 */
object RoleDescriptionDialog {

    fun show(activity: Activity, order: Int) {
        val members = NightFlow.membersOf(order).distinctBy { kindOf(it) }
        if (members.isEmpty()) return

        val panel = PanelDialog(activity, members.joinToString(" + ") { it.name })

        for (character in members) {
            val picture = ImageView(activity).apply {
                val res = RoleImages.of(character)
                if (res != 0) setImageResource(res)
                adjustViewBounds = true
            }
            panel.body.addView(picture, LinearLayout.LayoutParams(activity.dp(200), activity.dp(200)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })
            panel.body.addView(panel.text(character.name, 17f, activity.colorOf(R.color.cyan), true))
            panel.body.addView(panel.text(character.description, 15f))
        }

        panel.addButton(activity.getString(R.string.Generic_Okay), accent = true)
        panel.show()
    }
}
