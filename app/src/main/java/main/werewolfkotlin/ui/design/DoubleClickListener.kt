package main.werewolfkotlin.ui.design

import android.view.View

/** Click listener that also knows the double tap */
abstract class DoubleClickListener : View.OnClickListener {
    private var lastClick = 0L
    override fun onClick(v: View) {
        val now = System.currentTimeMillis()
        if (now - lastClick < 300) onDoubleClick()
        lastClick = now
    }
    abstract fun onDoubleClick()
}
