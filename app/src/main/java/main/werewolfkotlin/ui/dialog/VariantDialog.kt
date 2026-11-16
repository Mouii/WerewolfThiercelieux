package main.werewolfkotlin.ui.dialog

import main.werewolfkotlin.ui.design.colorOf

import android.app.Activity
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import main.werewolfkotlin.R
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.dp
import model.CharacterGame

/***
 * Window of the variants of a role : the key on top (arrows to change it), then the picture, then the description
 */
object VariantDialog {

    fun show(activity: Activity, variants: List<CharacterGame>, start: Int, onChoose: (CharacterGame) -> Unit) {
        var index = start

        //The title is the role, the selector shows the name of each variant
        val panel = PanelDialog(activity, variants.first().className)

        fun arrow(symbol: String): TextView = panel.text(symbol, 26f, activity.colorOf(R.color.cyan), true).apply {
            gravity = Gravity.CENTER
            setPadding(activity.dp(18), activity.dp(6), activity.dp(18), activity.dp(6))
            isClickable = true
        }

        val left = arrow("◀")
        val right = arrow("▶")
        val keyText = panel.text("", 20f, activity.colorOf(R.color.white), true).apply { gravity = Gravity.CENTER }

        val selector = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        selector.addView(left)
        selector.addView(keyText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        selector.addView(right)
        panel.body.addView(selector)

        val picture = ImageView(activity).apply { adjustViewBounds = true }
        panel.body.addView(picture, LinearLayout.LayoutParams(activity.dp(200), activity.dp(200)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        val description = panel.text("", 15f)
        panel.body.addView(description)

        fun render() {
            val variant = variants[index]
            keyText.text = variant.name
            val res = RoleImages.of(variant)
            if (res != 0) picture.setImageResource(res) else picture.setImageDrawable(null)
            description.text = variant.description
        }

        //Nothing to change with only one key
        val several = variants.size > 1
        left.isEnabled = several
        right.isEnabled = several
        left.alpha = if (several) 1f else 0.3f
        right.alpha = if (several) 1f else 0.3f
        left.setOnClickListener { index = (index - 1 + variants.size) % variants.size; render() }
        right.setOnClickListener { index = (index + 1) % variants.size; render() }

        render()

        panel.addButton(activity.getString(R.string.Generic_Cancel))
        panel.addButton(activity.getString(R.string.Variant_Choose), accent = true) { onChoose(variants[index]) }
        panel.show()
    }
}
