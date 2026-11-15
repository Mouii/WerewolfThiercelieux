package main.werewolfkotlin.ui.design

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.TextView

/***
 * Show a text in a box : the size is reduced until the text fits in it (wolf turn : a lot of text)
 */
fun TextView.setTextFitting(value: String, box: View) {
    text = value

    box.post {
        val width = box.width - paddingLeft - paddingRight
        val height = box.height - paddingTop - paddingBottom
        if (width <= 0 || height <= 0 || value.isEmpty()) return@post

        var size = 16f
        while (size > 9f) {
            val paint = TextPaint(this.paint)
            paint.textSize = size * resources.displayMetrics.scaledDensity
            @Suppress("DEPRECATION")
            val layout = StaticLayout(value, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
            if (layout.height <= height) break
            size -= 1f
        }
        textSize = size
    }
}
