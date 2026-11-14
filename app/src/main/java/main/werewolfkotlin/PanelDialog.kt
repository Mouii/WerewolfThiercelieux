package main.werewolfkotlin

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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

/***
 * A window with the look of the game screen : dark panel, cyan border, cyan title.
 * Used for the player, the turn order, the description of a role...
 */
class PanelDialog(
    private val activity: Activity,
    title: String,
    scrollable: Boolean = true
) {

    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()

    private val dialog = Dialog(activity)

    /** Put the content here */
    val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    private val footer = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
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
            setTextColor(CYAN)
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
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((activity.resources.displayMetrics.widthPixels * 0.92).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun dismiss() = dialog.dismiss()

    // ── Content helpers

    fun text(value: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    fun section(value: String): TextView =
        text(value, 12f, Color.parseColor("#8FA3B8")).apply { setPadding(0, dp(12), 0, dp(4)) }

    /***
     * Text field of the window
     */
    fun field(value: String, hint: String): EditText = EditText(activity).apply {
        setText(value)
        setHint(hint)
        setSelectAllOnFocus(true)
        setSingleLine()
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        setTextColor(Color.WHITE)
        setHintTextColor(Color.parseColor("#778899"))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundBg(Color.parseColor("#22FFFFFF"), Color.parseColor("#44FFFFFF"), dp(1), dp(10))
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
                if (selected) Color.argb(60, 0, 229, 255) else Color.parseColor("#22FFFFFF"),
                if (selected) CYAN else Color.parseColor("#33FFFFFF"),
                dp(if (selected) 2 else 1),
                dp(10)
            )
            isClickable = true
            setOnClickListener { onClick() }
        }
        if (iconRes != 0) {
            row.addView(ImageView(activity).apply { setImageResource(iconRes) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
        row.addView(TextView(activity).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })

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
            if (selected) Color.argb(50, 0, 229, 255) else Color.TRANSPARENT,
            if (selected) CYAN else Color.parseColor("#33FFFFFF"),
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
                setTextColor(Color.WHITE)
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
            setTextColor(CYAN)
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundBg(Color.argb(60, 0, 229, 255), CYAN, dp(1), dp(10))
            isClickable = true
        }
        if (iconRes != 0) {
            header.addView(ImageView(activity).apply { setImageResource(iconRes) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
        header.addView(TextView(activity).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
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
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            isClickable = true
            background = roundBg(
                if (accent) Color.argb(70, 0, 229, 255) else Color.parseColor("#22FFFFFF"),
                if (accent) CYAN else Color.parseColor("#66FFFFFF"),
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

    private fun roundBg(fillColor: Int, strokeColor: Int, strokeWidth: Int, radius: Int) = GradientDrawable().apply {
        setColor(fillColor)
        setStroke(strokeWidth, strokeColor)
        cornerRadius = radius.toFloat()
    }

    companion object {
        val CYAN: Int = Color.parseColor("#00E5FF")

        /** Dark panel with a cyan border : the look of the game screen */
        fun panelBackground(context: Context): GradientDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#F00B1220"))
            setStroke((1 * context.resources.displayMetrics.density).toInt(), Color.parseColor("#6600E5FF"))
            cornerRadius = 16 * context.resources.displayMetrics.density
        }
    }
}

/***
 * Button with the look of the game screens : translucent fill, coloured border, rounded
 */
fun panelButton(context: Context, label: String, accent: Int = PanelDialog.CYAN): MaterialButton {
    val density = context.resources.displayMetrics.density
    return MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        strokeColor = ColorStateList.valueOf(accent)
        strokeWidth = (1 * density).toInt()
        cornerRadius = (10 * density).toInt()
        backgroundTintList = ColorStateList.valueOf(Color.argb(51, Color.red(accent), Color.green(accent), Color.blue(accent)))
    }
}
