package main.werewolfkotlin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import model.CharacterGame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/***
 * Draws every player as a stickman bust (head + shoulders) on a circle.
 * The players whose role is currently called get a neon "HUD" look, the others are dimmed.
 */
class PlayerCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Called with the index of the tapped seat */
    var onSeatTapped: ((Int) -> Unit)? = null

    private var seatList: List<Seat> = emptyList()
    private var active: Set<CharacterGame> = emptySet()
    private var marked: Set<CharacterGame> = emptySet()

    private val density = resources.displayMetrics.density
    private val cyan = Color.parseColor("#00E5FF")
    private val idle = Color.parseColor("#9FB3C8")
    private val deadGrey = Color.parseColor("#5B6470")
    private val red = Color.parseColor("#FF5252")

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val shoulders = Path()
    private val arcBounds = RectF()

    private var xs = FloatArray(0)
    private var ys = FloatArray(0)
    private var avatarW = 0f
    private var dense = false
    private val purple = Color.parseColor("#B388FF")
    private var glowFilter: BlurMaskFilter? = null
    private var glowFilterSize = -1f

    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        //BlurMaskFilter (the neon glow) needs the software renderer
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /***
     * Give the new state to draw
     */
    fun update(seats: List<Seat>, activeCharacters: Collection<CharacterGame>, markedDead: Collection<CharacterGame>) {
        seatList = seats.toList()
        active = activeCharacters.toHashSet()
        marked = markedDead.toHashSet()

        if (active.isEmpty()) animator.cancel()
        else if (!animator.isStarted) animator.start()

        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (active.isNotEmpty() && !animator.isStarted) animator.start()
    }

    /***
     * Place the players on one circle, or on several concentric circles when there are many of them
     * (up to 46 cards). Player 1 is at the top, then clockwise, from the outside to the inside.
     */
    private fun computeLayout() {
        val n = seatList.size
        if (xs.size != n) {
            xs = FloatArray(n)
            ys = FloatArray(n)
        }
        if (width == 0 || height == 0 || n == 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val base = min(width, height) / 2f

        if (n == 1) {
            avatarW = base * 0.5f
            dense = false
            xs[0] = centerX
            ys[0] = centerY
            return
        }

        //Find the biggest avatar size for which all the players fit on the rings
        val minW = 20f * density
        var w = base * 0.42f
        var rings = ringsFor(base, w)
        while (rings.sumOf { it.second } < n && w > minW) {
            w = max(w * 0.93f, minW)
            rings = ringsFor(base, w)
        }
        avatarW = w
        dense = w < 44f * density

        //Use as few rings as needed, and share the players between them according to their length
        var used = 0
        var capacity = 0
        while (used < rings.size && capacity < n) {
            capacity += rings[used].second
            used++
        }
        used = max(used, 1)
        val usedRings = rings.take(used)
        val totalCap = usedRings.sumOf { it.second }
        val counts = IntArray(used) { (n.toFloat() * usedRings[it].second / totalCap).toInt() }
        var missing = n - counts.sum()
        var k = 0
        while (missing > 0) {
            counts[k % used]++
            missing--
            k++
        }

        var index = 0
        for (ring in 0 until used) {
            val radius = usedRings[ring].first
            val count = counts[ring]
            //Every other ring is shifted, so the players are not aligned
            val shift = if (ring % 2 == 1) 0.5 else 0.0
            for (j in 0 until count) {
                if (index >= n) break
                val angle = 2.0 * PI * (j + shift) / max(count, 1) - PI / 2
                xs[index] = centerX + (radius * cos(angle)).toFloat()
                ys[index] = centerY + (radius * sin(angle)).toFloat()
                index++
            }
        }

        if (glowFilterSize != w) {
            glowFilterSize = w
            glowFilter = BlurMaskFilter(max(w * 0.12f, 2f), BlurMaskFilter.Blur.NORMAL)
        }
    }

    /***
     * Radius and capacity of the rings, from the outside to the inside, for an avatar width
     */
    private fun ringsFor(base: Float, w: Float): List<Pair<Float, Int>> {
        val result = mutableListOf<Pair<Float, Int>>()
        var r = base - w * 0.85f
        val gap = w * 1.7f
        while (r > w * 0.5f) {
            val capacity = floor(2f * PI.toFloat() * r / (w * 1.25f)).toInt()
            result.add(Pair(r, max(capacity, 1)))
            r -= gap
        }
        if (result.isEmpty()) result.add(Pair(0f, 1))
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeLayout()
        val anyActive = active.isNotEmpty()
        for ((i, seat) in seatList.withIndex()) {
            drawSeat(canvas, seat, xs[i], ys[i], anyActive)
        }
    }

    private fun drawSeat(c: Canvas, seat: Seat, x: Float, y: Float, anyActive: Boolean) {
        val w = avatarW
        val character = seat.character
        val isActive = character != null && character in active
        val isMarked = character != null && character in marked

        val color = when {
            !seat.alive -> deadGrey
            isActive -> cyan
            else -> idle
        }
        val alpha = when {
            !seat.alive -> 100
            anyActive && !isActive -> 100
            character == null -> 150
            else -> 255
        }

        val strokeWidth = max(w * 0.06f, 2f)
        val headR = w * 0.2f
        val headY = y - w * 0.18f

        shoulders.reset()
        shoulders.moveTo(x - w * 0.42f, y + w * 0.32f)
        shoulders.cubicTo(x - w * 0.42f, y + w * 0.02f, x + w * 0.42f, y + w * 0.02f, x + w * 0.42f, y + w * 0.32f)

        if (isActive) {
            val pulse = 0.6f + 0.4f * sin(phase * 2f * PI.toFloat())

            //Neon glow behind the bust
            glow.color = cyan
            glow.alpha = (220 * pulse).toInt()
            glow.strokeWidth = strokeWidth * 2.4f
            glow.maskFilter = glowFilter
            c.drawCircle(x, headY, headR, glow)
            c.drawPath(shoulders, glow)

            //Translucent head
            fill.color = cyan
            fill.alpha = 70
            c.drawCircle(x, headY, headR, fill)

            //HUD ring : thin circle and two scanning arcs
            val ring = w * 0.62f
            stroke.color = cyan
            stroke.strokeWidth = max(strokeWidth * 0.5f, 1.5f)
            stroke.alpha = 70
            c.drawCircle(x, y, ring, stroke)
            stroke.alpha = 255
            arcBounds.set(x - ring, y - ring, x + ring, y + ring)
            c.drawArc(arcBounds, phase * 360f, 60f, false, stroke)
            c.drawArc(arcBounds, phase * 360f + 180f, 60f, false, stroke)
        }

        //Stickman bust
        stroke.color = color
        stroke.alpha = alpha
        stroke.strokeWidth = strokeWidth
        c.drawCircle(x, headY, headR, stroke)
        c.drawPath(shoulders, stroke)

        //Marked as dead (waiting for the confirmation at the end of the day)
        if (isMarked) {
            stroke.color = red
            stroke.alpha = 255
            stroke.strokeWidth = max(strokeWidth * 0.7f, 2f)
            c.drawCircle(x, y, w * 0.62f, stroke)
        }

        //Definitely dead
        if (!seat.alive) {
            stroke.color = red
            stroke.alpha = 180
            stroke.strokeWidth = strokeWidth
            c.drawLine(x - w * 0.38f, y - w * 0.38f, x + w * 0.38f, y + w * 0.38f, stroke)
        }

        //Infected by the werewolves
        if (character != null && character.infected) {
            fill.color = purple
            fill.alpha = 255
            c.drawCircle(x + w * 0.4f, y - w * 0.4f, max(w * 0.11f, 3f), fill)
        }

        //Name and role
        text.isFakeBoldText = isActive
        text.color = Color.WHITE
        text.alpha = alpha
        text.textSize = max(w * 0.2f, 9f * density)
        val name = TextUtils.ellipsize(seat.name, text, w * 1.3f, TextUtils.TruncateAt.END).toString()
        c.drawText(name, x, y + w * 0.58f, text)

        //With many players there is no room for the role, the neon glow shows who is called
        if (!dense) {
            text.color = if (isActive) cyan else idle
            text.alpha = alpha
            text.textSize = w * 0.16f
            val role = TextUtils.ellipsize(character?.name ?: "—", text, w * 1.4f, TextUtils.TruncateAt.END).toString()
            c.drawText(role, x, y + w * 0.78f, text)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                computeLayout()
                val nearest = seatList.indices.minByOrNull { hypot(event.x - xs[it], event.y - ys[it]) }
                if (nearest != null && hypot(event.x - xs[nearest], event.y - ys[nearest]) <= avatarW * 0.75f) {
                    performClick()
                    onSeatTapped?.invoke(nearest)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
