package main.werewolfkotlin.ui.view

import main.werewolfkotlin.R

import main.werewolfkotlin.ui.design.colorOf

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import main.werewolfkotlin.game.Seat
import model.CharacterGame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/***
 * Draws every player as a stickman bust (head + shoulders), all on one ellipse that uses the whole panel.
 * Pinch to zoom, drag to move when zoomed : useful when there are many players.
 *
 * The colour is on the drawing itself (no circle around it) :
 * - Cyan : the role is called now.
 * - Red : target of the called role. Green : player who selected the called role.
 * - Purple : player of the block of the deaths. Yellow : player without role.
 */
class PlayerCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Called with the index of the tapped seat */
    var onSeatTapped: ((Int) -> Unit)? = null

    /** Called with the index of the seat held with the finger */
    var onSeatLongPressed: ((Int) -> Unit)? = null

    private var seatList: List<Seat> = emptyList()
    private var active: Set<CharacterGame> = emptySet()
    private var purpleSeats: Set<Seat> = emptySet()
    private var redSeats: Set<Seat> = emptySet()
    private var greenSeats: Set<Seat> = emptySet()
    private var badges: Map<Seat, Int> = emptyMap()

    private val density = resources.displayMetrics.density

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
    private val drawables = mutableMapOf<Int, Drawable?>()

    //Positions in the panel (before zoom)
    private var xs = FloatArray(0)
    private var ys = FloatArray(0)
    private var avatarW = 0f
    private var glowFilter: BlurMaskFilter? = null
    private var glowFilterSize = -1f

    //Zoom : a point of the panel goes to centre + (point - centre) * zoom + pan
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f

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

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newZoom = (zoom * detector.scaleFactor).coerceIn(1f, maxZoom())
            val cx = width / 2f
            val cy = height / 2f
            val ratio = newZoom / zoom

            //The point under the fingers stays under the fingers
            panX = detector.focusX - cx - (detector.focusX - cx - panX) * ratio
            panY = detector.focusY - cy - (detector.focusY - cy - panY) * ratio
            zoom = newZoom
            clampPan()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (zoom > 1f && !scaleDetector.isInProgress) {
                panX -= distanceX
                panY -= distanceY
                clampPan()
                invalidate()
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            seatAt(e.x, e.y)?.let { performClick(); onSeatTapped?.invoke(it) }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            seatAt(e.x, e.y)?.let { onSeatLongPressed?.invoke(it) }
        }
    })

    init {
        //BlurMaskFilter (the neon glow) needs the software renderer
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /***
     * Give the new state to draw
     */
    fun update(
        seats: List<Seat>,
        activeCharacters: Collection<CharacterGame>,
        redLights: Set<Seat>,
        greenLights: Set<Seat>,
        purpleLights: Set<Seat>,
        roleBadges: Map<Seat, Int>
    ) {
        seatList = seats.toList()
        active = activeCharacters.toHashSet()
        redSeats = redLights
        greenSeats = greenLights
        purpleSeats = purpleLights
        badges = roleBadges

        updateAnimation()
        invalidate()
    }

    private fun updateAnimation() {
        val needed = active.isNotEmpty() || redSeats.isNotEmpty() || greenSeats.isNotEmpty() || purpleSeats.isNotEmpty()
        if (!needed) animator.cancel()
        else if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    // ──────────────────────────────────────────────────────────────
    //  Layout : one ellipse
    // ──────────────────────────────────────────────────────────────

    /***
     * Put the players on one ellipse, evenly spaced. Player 1 is at the top, then clockwise.
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
        val maxW = min(width, height) * 0.21f
        val minW = 12f * density

        if (n == 1) {
            avatarW = maxW
            xs[0] = centerX
            ys[0] = centerY
            return
        }

        //The size of the avatars and the size of the ellipse depend on each other
        var w = maxW
        var rx = 0f
        var ry = 0f
        var length = 0f
        repeat(4) {
            rx = max(width / 2f - w * 0.75f, 1f)
            ry = max(height / 2f - w * 0.95f, 1f)
            length = ellipseLength(rx, ry)
            w = (length / n * 0.9f).coerceIn(minW, maxW)
        }
        avatarW = w

        //Points at the same distance from each other along the ellipse
        val steps = 1440
        val cumulative = FloatArray(steps + 1)
        val top = (-PI / 2).toFloat()
        var previousX = rx * cos(top)
        var previousY = ry * sin(top)
        for (i in 1..steps) {
            val t = (-PI / 2 + 2.0 * PI * i / steps).toFloat()
            val px = rx * cos(t)
            val py = ry * sin(t)
            cumulative[i] = cumulative[i - 1] + hypot(px - previousX, py - previousY)
            previousX = px
            previousY = py
        }

        val total = cumulative[steps]
        var cursor = 0
        for (i in 0 until n) {
            val wanted = total * i / n
            while (cursor < steps && cumulative[cursor + 1] < wanted) cursor++
            val span = cumulative[min(cursor + 1, steps)] - cumulative[cursor]
            val part = if (span > 0f) (wanted - cumulative[cursor]) / span else 0f
            val t = (-PI / 2 + 2.0 * PI * (cursor + part) / steps).toFloat()
            xs[i] = centerX + rx * cos(t)
            ys[i] = centerY + ry * sin(t)
        }

        if (glowFilterSize != w) {
            glowFilterSize = w
            glowFilter = BlurMaskFilter(max(w * 0.12f, 2f), BlurMaskFilter.Blur.NORMAL)
        }
    }

    /** Length of an ellipse (Ramanujan) */
    private fun ellipseLength(rx: Float, ry: Float): Float {
        val h = ((rx - ry) * (rx - ry)) / ((rx + ry) * (rx + ry))
        return (PI * (rx + ry) * (1 + 3 * h / (10 + kotlin.math.sqrt(4 - 3 * h)))).toFloat()
    }

    // ──────────────────────────────────────────────────────────────
    //  Zoom
    // ──────────────────────────────────────────────────────────────

    /** Zoom for which the avatars are comfortable to touch */
    private fun maxZoom(): Float {
        if (avatarW <= 0f) return 3f
        return (64f * density / avatarW).coerceIn(3f, 8f)
    }

    private fun clampPan() {
        if (zoom <= 1.01f) {
            zoom = 1f
            panX = 0f
            panY = 0f
            return
        }
        val limitX = (zoom - 1f) * width / 2f
        val limitY = (zoom - 1f) * height / 2f
        panX = panX.coerceIn(-limitX, limitX)
        panY = panY.coerceIn(-limitY, limitY)
    }

    /***
     * Seat under a finger, null if there is none
     */
    private fun seatAt(touchX: Float, touchY: Float): Int? {
        if (seatList.isEmpty()) return null
        computeLayout()

        //Back to the panel, before the zoom
        val cx = width / 2f
        val cy = height / 2f
        val x = (touchX - cx - panX) / zoom + cx
        val y = (touchY - cy - panY) / zoom + cy

        val nearest = seatList.indices.minByOrNull { hypot(x - xs[it], y - ys[it]) } ?: return null
        val reach = max(avatarW * 0.75f, 18f * density / zoom)
        return if (hypot(x - xs[nearest], y - ys[nearest]) <= reach) nearest else null
    }

    // ──────────────────────────────────────────────────────────────
    //  Drawing
    // ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeLayout()
        clampPan()

        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoom, zoom, width / 2f, height / 2f)

        val anyActive = active.isNotEmpty()
        for ((i, seat) in seatList.withIndex()) {
            drawSeat(canvas, seat, xs[i], ys[i], anyActive)
        }
        canvas.restore()
    }

    private fun drawSeat(c: Canvas, seat: Seat, x: Float, y: Float, anyActive: Boolean) {
        val w = avatarW
        val character = seat.character
        val isActive = seat.alive && character != null && character in active
        val isRed = seat.alive && seat in redSeats
        val isGreen = seat.alive && seat in greenSeats
        val isPurple = seat.alive && seat in purpleSeats
        val lit = isActive || isRed || isGreen || isPurple

        //The drawing itself takes the colour. Several states : a glow for each one
        val primary = when {
            !seat.alive -> colorOf(R.color.player_dead)
            isRed -> colorOf(R.color.light_red)
            isGreen -> colorOf(R.color.light_green)
            isPurple -> colorOf(R.color.player_death_mark)
            isActive -> colorOf(R.color.cyan)
            character == null -> colorOf(R.color.no_role)
            else -> colorOf(R.color.player_idle)
        }
        val alpha = when {
            !seat.alive -> 100
            lit || character == null -> 255
            anyActive -> 100
            else -> 255
        }

        val strokeWidth = max(w * 0.06f, 1.5f) * (if (lit) 1.5f else 1f)
        val headR = w * 0.2f
        val headY = y - w * 0.18f
        val pulse = 0.6f + 0.4f * sin(phase * 2f * PI.toFloat())

        shoulders.reset()
        shoulders.moveTo(x - w * 0.42f, y + w * 0.32f)
        shoulders.cubicTo(x - w * 0.42f, y + w * 0.02f, x + w * 0.42f, y + w * 0.02f, x + w * 0.42f, y + w * 0.32f)

        //Glow along the drawing, one pass for each state
        val glows = mutableListOf<Int>()
        if (isActive) glows.add(colorOf(R.color.cyan))
        if (isRed) glows.add(colorOf(R.color.light_red))
        if (isGreen) glows.add(colorOf(R.color.light_green))
        if (isPurple) glows.add(colorOf(R.color.player_death_mark))
        for (color in glows) {
            glow.color = color
            glow.alpha = (230 * pulse).toInt()
            glow.strokeWidth = strokeWidth * 2.2f
            glow.maskFilter = glowFilter
            c.drawCircle(x, headY, headR, glow)
            c.drawPath(shoulders, glow)
        }

        //Head filled with the colour
        if (lit) {
            fill.color = primary
            fill.alpha = 110
            c.drawCircle(x, headY, headR, fill)
        }

        //Stickman bust
        stroke.color = primary
        stroke.alpha = alpha
        stroke.strokeWidth = strokeWidth
        c.drawCircle(x, headY, headR, stroke)
        c.drawPath(shoulders, stroke)

        //Dead : a cross on the drawing
        if (!seat.alive) {
            stroke.color = colorOf(R.color.player_dead_cross)
            stroke.alpha = 200
            stroke.strokeWidth = strokeWidth
            c.drawLine(x - w * 0.34f, y - w * 0.40f, x + w * 0.34f, y + w * 0.32f, stroke)
            c.drawLine(x + w * 0.34f, y - w * 0.40f, x - w * 0.34f, y + w * 0.32f, stroke)
        }

        //Infected by the werewolves
        if (character != null && character.isInfected) {
            fill.color = colorOf(R.color.player_infected)
            fill.alpha = 255
            c.drawCircle(x + w * 0.4f, y - w * 0.4f, max(w * 0.11f, 2f), fill)
        }

        //Small picture of the role (wolf turn : many roles are awake)
        val badge = badges[seat]
        if (badge != null && badge != 0) drawBadge(c, badge, x + w * 0.36f, y + w * 0.14f, w * 0.52f)

        //Names and roles are only readable when the avatars are big enough on screen
        val onScreen = w * zoom
        if (onScreen >= 30f * density) {
            text.isFakeBoldText = lit
            text.color = if (character == null) colorOf(R.color.no_role) else colorOf(R.color.white)
            text.alpha = alpha
            text.textSize = max(w * 0.2f, 9f * density / zoom)
            val name = TextUtils.ellipsize(seat.name, text, w * 1.3f, TextUtils.TruncateAt.END).toString()
            c.drawText(name, x, y + w * 0.58f, text)

            if (onScreen >= 56f * density) {
                text.isFakeBoldText = false
                text.color = if (lit) primary else colorOf(R.color.player_idle)
                text.alpha = alpha
                text.textSize = max(w * 0.16f, 8f * density / zoom)
                val role = TextUtils.ellipsize(character?.name ?: "—", text, w * 1.4f, TextUtils.TruncateAt.END).toString()
                c.drawText(role, x, y + w * 0.78f, text)
            }
        }
    }

    private fun drawBadge(c: Canvas, res: Int, x: Float, y: Float, size: Float) {
        val drawable = drawables.getOrPut(res) { ContextCompat.getDrawable(context, res) } ?: return
        val half = (size / 2f).toInt()
        drawable.setBounds(x.toInt() - half, y.toInt() - half, x.toInt() + half, y.toInt() + half)
        drawable.draw(c)

        stroke.color = colorOf(R.color.white)
        stroke.alpha = 220
        stroke.strokeWidth = max(size * 0.05f, 1f)
        c.drawRect(x - half, y - half, x + half, y + half, stroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
