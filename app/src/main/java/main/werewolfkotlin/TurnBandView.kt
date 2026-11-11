package main.werewolfkotlin

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

/***
 * One step of the turn order (a role, or a group of roles playing at the same moment)
 */
data class BandEntry(
    val order: Int,
    val label: String,
    val iconRes: Int,
    val awake: Boolean,            //Awake at night (for the current night)
    val pendingAwake: Boolean?     //Change waiting for the end of the night, null if none
)

/***
 * Band showing the turn order as a line of cards.
 * - Tap a card to call (or call again) the role.
 * - Long press a card to wake it up / put it to sleep.
 * - The "Turn order" button opens the window where the order can be changed.
 */
class TurnBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onSelect: ((Int) -> Unit)? = null
    var onToggleAwake: ((Int) -> Unit)? = null
    var onOpenOrder: (() -> Unit)? = null

    private val cyan = Color.parseColor("#00E5FF")
    private val orange = Color.parseColor("#FFB300")

    private var entries: List<BandEntry> = emptyList()
    private var currentOrder: Int? = null

    private val chipRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
    }
    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(chipRow)
    }

    init {
        orientation = VERTICAL
        setPadding(dp(8), dp(6), dp(8), dp(8))
        background = roundBg(Color.parseColor("#CC0B1220"), Color.parseColor("#6600E5FF"), dp(1), dp(16))

        //Header : title and button of the turn order
        val header = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(context).apply {
            setText(R.string.Band_Title)
            setTextColor(cyan)
            setTextSize(12f)
            letterSpacing = 0.1f
        }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val orderButton = TextView(context).apply {
            setText(R.string.Band_OrderButton)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(13f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            background = roundBg(Color.argb(60, 0, 229, 255), cyan, dp(1), dp(10))
            setOnClickListener { onOpenOrder?.invoke() }
        }
        header.addView(orderButton)

        addView(header)
        addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    /***
     * Give the new state to display
     */
    fun setEntries(list: List<BandEntry>, currentOrder: Int?) {
        this.entries = list
        this.currentOrder = currentOrder
        buildChips()
    }

    private fun buildChips() {
        chipRow.removeAllViews()
        var currentChip: View? = null

        for (entry in entries) {
            val isCurrent = entry.order == currentOrder
            val chip = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(6), dp(6), dp(6), dp(6))
                background = roundBg(
                    if (isCurrent) Color.argb(60, 0, 229, 255) else Color.parseColor("#22FFFFFF"),
                    if (isCurrent) cyan else Color.parseColor("#44FFFFFF"),
                    dp(if (isCurrent) 2 else 1),
                    dp(12)
                )
                alpha = if (entry.awake) 1f else 0.45f
                setOnClickListener { onSelect?.invoke(entry.order) }
                setOnLongClickListener { showMenu(this, entry); true }
            }

            val icon = ImageView(context)
            if (entry.iconRes != 0) icon.setImageResource(entry.iconRes)
            chip.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)))

            val label = TextView(context).apply {
                val pending = entry.pendingAwake != null
                text = (if (pending) "● " else "") + entry.label
                setTextColor(if (pending) orange else Color.WHITE)
                setTextSize(10f)
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            chip.addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            chipRow.addView(chip, LinearLayout.LayoutParams(dp(78), LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4), 0, dp(4), 0) })
            if (isCurrent) currentChip = chip
        }

        //Keep the called role in view
        currentChip?.let { chip -> scroll.post { scroll.smoothScrollTo(chip.left - dp(40), 0) } }
    }

    private fun showMenu(anchor: View, entry: BandEntry) {
        val shownAwake = entry.pendingAwake ?: entry.awake
        val popup = PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, if (shownAwake) R.string.Band_PutToSleep else R.string.Band_WakeUp)
        popup.setOnMenuItemClickListener {
            onToggleAwake?.invoke(entry.order)
            true
        }
        popup.show()
    }

    // ── Small helpers
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun roundBg(fillColor: Int, strokeColor: Int, strokeWidth: Int, radius: Int) = GradientDrawable().apply {
        setColor(fillColor)
        setStroke(strokeWidth, strokeColor)
        cornerRadius = radius.toFloat()
    }
}
