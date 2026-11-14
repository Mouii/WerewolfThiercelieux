package main.werewolfkotlin

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
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
 * - Tap a card : call the role at night, show its lights otherwise.
 * - Double tap a card : its picture and its description.
 * - Long press a card : wake it up / put it to sleep.
 * - The "Turn order" button opens the window where the order can be changed.
 */
class TurnBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onSelect: ((Int) -> Unit)? = null
    var onDescribe: ((Int) -> Unit)? = null
    var onToggleAwake: ((Int) -> Unit)? = null
    var onOpenOrder: (() -> Unit)? = null

    private val cyan = Color.parseColor("#00E5FF")
    private val orange = Color.parseColor("#FFB300")

    private var entries: List<BandEntry> = emptyList()
    private var highlightOrder: Int? = null

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

        //The button of the turn order is also the title of the band, in the centre
        val header = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        val orderButton = TextView(context).apply {
            setText(R.string.Band_OrderButton)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(14f)
            letterSpacing = 0.08f
            setPadding(dp(24), dp(7), dp(24), dp(7))
            isClickable = true
            background = roundBg(Color.argb(60, 0, 229, 255), cyan, dp(1), dp(10))
            setOnClickListener { onOpenOrder?.invoke() }
        }
        header.addView(orderButton)

        addView(header)
        addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        //Small hint under the roles
        addView(TextView(context).apply {
            setText(R.string.Band_Hint)
            setTextColor(Color.parseColor("#8FA3B8"))
            setTextSize(10f)
            setPadding(dp(4), dp(2), dp(4), 0)
        })
    }

    /***
     * Give the new state to display
     */
    fun setEntries(list: List<BandEntry>, highlightOrder: Int?) {
        this.entries = list
        this.highlightOrder = highlightOrder
        buildChips()
    }

    private fun buildChips() {
        chipRow.removeAllViews()
        var currentChip: View? = null

        for (entry in entries) {
            val isCurrent = entry.order == highlightOrder
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
            }

            //Tap, double tap and long press are told apart by the detector
            val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onSelect?.invoke(entry.order)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onDescribe?.invoke(entry.order)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    showMenu(chip, entry)
                }
            })
            chip.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                true
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
