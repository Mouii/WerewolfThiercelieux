package main.werewolfkotlin.ui.dialog

import android.app.Activity
import main.werewolfkotlin.R

/***
 * A question with Cancel / Yes. The action is done on Yes.
 */
fun confirmDialog(activity: Activity, title: String, question: String, onYes: () -> Unit) {
    val panel = PanelDialog(activity, title)
    panel.body.addView(panel.text(question))
    panel.addButton(activity.getString(R.string.Generic_Cancel))
    panel.addButton(activity.getString(R.string.Generic_Yes), accent = true) { onYes() }
    panel.show()
}
