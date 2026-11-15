package main.werewolfkotlin.ui.design

import main.werewolfkotlin.R

import android.content.Context
import android.graphics.drawable.GradientDrawable

/** Rounded rectangle with a border. Sizes in pixels */
fun roundBg(fillColor: Int, strokeColor: Int, strokeWidth: Int, radius: Int) = GradientDrawable().apply {
    setColor(fillColor)
    setStroke(strokeWidth, strokeColor)
    cornerRadius = radius.toFloat()
}

/** Dark panel with a cyan border : the look of the game screen */
fun panelBackground(context: Context): GradientDrawable =
    roundBg(context.colorOf(R.color.panel_fill), context.colorOf(R.color.cyan_stroke), context.dp(1), context.dp(16))
