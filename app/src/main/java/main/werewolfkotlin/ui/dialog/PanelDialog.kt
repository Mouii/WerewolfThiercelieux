package main.werewolfkotlin.ui.dialog

import main.werewolfkotlin.R

import main.werewolfkotlin.ui.design.colorOf

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.core.graphics.ColorUtils
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.content.res.ColorStateList
import com.google.android.material.button.MaterialButton
import main.werewolfkotlin.ui.design.dp
import main.werewolfkotlin.ui.design.panelBackground
import main.werewolfkotlin.ui.design.roundBg

/***
 * A window with the look of the game screen : dark panel, cyan border, cyan title.
 * Used for the player, the turn order, the description of a role...
 */
class PanelDialog(
    private val activity: Activity,
    title: String,
    scrollable: Boolean = true
) {

    private fun dp(value: Int): Int = activity.dp(value)

    private val dialog = Dialog(activity)

    /** Put the content here */
    val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    private val footer = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
    }

    /** Small text between the content and the buttons */
    private val hintView = TextView(activity).apply {
        textSize = 12f
        visibility = View.GONE
        setPadding(0, dp(8), 0, 0)
    }

    /** Called when the window closes, whatever the way */
    var onDismiss: (() -> Unit)? = null

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBackground(activity)
            setPadding(dp(16), dp(14), dp(16), dp(12))
        }

        root.addView(TextView(activity).apply {
            text = title
            setTextColor(activity.colorOf(R.color.cyan))
            textSize = 14f
            letterSpacing = 0.08f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val content: View = if (scrollable) {
            MaxHeightScrollView(activity, (activity.resources.displayMetrics.heightPixels * 0.6).toInt()).apply { addView(body) }
        } else {
            body
        }
        root.addView(content, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })

        root.addView(hintView)

        root.addView(footer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        dialog.setContentView(root)
        dialog.setOnDismissListener { onDismiss?.invoke() }
    }

    fun show() {
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(activity.colorOf(R.color.transparent)))
            setLayout((activity.resources.displayMetrics.widthPixels * 0.92).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun dismiss() = dialog.dismiss()

    /***
     * Show a small hint between the content and the buttons. null hides it
     */
    fun setHint(value: String?, color: Int = activity.colorOf(R.color.hint)) {
        if (value == null) {
            hintView.visibility = View.GONE
        } else {
            hintView.text = value
            hintView.setTextColor(color)
            hintView.visibility = View.VISIBLE
        }
    }

    // ── Content helpers

    fun text(value: String, size: Float = 15f, color: Int = activity.colorOf(R.color.white), bold: Boolean = false): TextView =
        TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    fun section(value: String): TextView =
        text(value, 12f, activity.colorOf(R.color.hint)).apply { setPadding(0, dp(12), 0, dp(4)) }

    /***
     * Text field of the window
     */
    fun field(value: String, hint: String): EditText = EditText(activity).apply {
        setText(value)
        setHint(hint)
        setSelectAllOnFocus(true)
        setSingleLine()
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        setTextColor(activity.colorOf(R.color.white))
        setHintTextColor(activity.colorOf(R.color.placeholder))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundBg(activity.colorOf(R.color.glass_fill), activity.colorOf(R.color.glass_stroke), dp(1), dp(10))
    }

    /***
     * A small picture (if there is one) and a label that takes the rest of the line
     */
    private fun addIconAndLabel(line: LinearLayout, iconRes: Int, label: String) {
        if (iconRes != 0) {
            line.addView(ImageView(activity).apply { setImageResource(iconRes) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
        line.addView(TextView(activity).apply {
            text = label
            textSize = 15f
            setTextColor(activity.colorOf(R.color.white))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
    }

    /***
     * A line to choose : a picture, a name. Tapping it does the action
     */
    fun choiceRow(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundBg(
                if (selected) activity.colorOf(R.color.cyan_fill_selected) else activity.colorOf(R.color.glass_fill),
                if (selected) activity.colorOf(R.color.cyan) else activity.colorOf(R.color.glass_stroke_soft),
                dp(if (selected) 2 else 1),
                dp(10)
            )
            isClickable = true
            setOnClickListener { onClick() }
        }
        addIconAndLabel(row, iconRes, label)

        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6) }
        return row
    }

    /***
     * A card : just the picture of the role. A role without picture shows its name
     */
    fun cardTile(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit): View {
        val border = roundBg(
            if (selected) activity.colorOf(R.color.cyan_fill_tile) else activity.colorOf(R.color.transparent),
            if (selected) activity.colorOf(R.color.cyan) else activity.colorOf(R.color.glass_stroke_soft),
            dp(if (selected) 3 else 1),
            dp(8)
        )
        val tile: View = if (iconRes != 0) {
            ImageView(activity).apply {
                setImageResource(iconRes)
                adjustViewBounds = true
                contentDescription = label
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
        } else {
            TextView(activity).apply {
                text = label
                textSize = 11f
                setTextColor(activity.colorOf(R.color.white))
                gravity = Gravity.CENTER
                minHeight = dp(80)
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
        }
        tile.background = border
        tile.isClickable = true
        tile.setOnClickListener { onClick() }
        return tile
    }

    /***
     * The cards side by side, 4 per line
     */
    fun cardGrid(tiles: List<View>, perRow: Int = 4): View {
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        for (line in tiles.chunked(perRow)) {
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0 until perRow) {
                val cell: View = if (i < line.size) line[i] else View(activity)
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            box.addView(row)
        }
        return box
    }

    /***
     * A bar with a picture and a title that opens or closes the content below it when it is tapped.
     * The content starts closed.
     */
    fun collapsible(iconRes: Int, label: String, content: View): View {
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

        val arrow = TextView(activity).apply {
            text = "▸"
            textSize = 16f
            setTextColor(activity.colorOf(R.color.cyan))
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundBg(activity.colorOf(R.color.cyan_fill_selected), activity.colorOf(R.color.cyan), dp(1), dp(10))
            isClickable = true
        }
        addIconAndLabel(header, iconRes, label)
        header.addView(arrow)

        content.visibility = View.GONE
        header.setOnClickListener {
            val open = content.visibility != View.VISIBLE
            content.visibility = if (open) View.VISIBLE else View.GONE
            arrow.text = if (open) "▾" else "▸"
        }

        box.addView(header)
        box.addView(content)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6) }
        return box
    }

    /***
     * Button of the bottom of the window. By default it closes the window after its action.
     */
    fun addButton(label: String, accent: Boolean = false, closes: Boolean = true, onClick: () -> Unit = {}): TextView {
        val button = TextView(activity).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(activity.colorOf(R.color.white))
            setPadding(dp(14), dp(9), dp(14), dp(9))
            isClickable = true
            background = roundBg(
                if (accent) activity.colorOf(R.color.cyan_fill_accent) else activity.colorOf(R.color.glass_fill),
                if (accent) activity.colorOf(R.color.cyan) else activity.colorOf(R.color.glass_stroke_strong),
                dp(1), dp(10)
            )
            setOnClickListener {
                onClick()
                if (closes) dismiss()
            }
        }
        footer.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })
        return button
    }

    /***
     * Scroll view that never gets higher than a maximum
     */
    private class MaxHeightScrollView(context: Context, private val maxHeightPx: Int) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST))
        }
    }
}

/***
 * Button with the look of the game screens : translucent fill, coloured border, rounded
 */
fun panelButton(context: Context, label: String, accent: Int = context.colorOf(R.color.cyan)): MaterialButton {
    return MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        text = label
        isAllCaps = false
        setTextColor(context.colorOf(R.color.white))
        strokeColor = ColorStateList.valueOf(accent)
        strokeWidth = context.dp(1)
        cornerRadius = context.dp(10)
        backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 51))
    }
}
