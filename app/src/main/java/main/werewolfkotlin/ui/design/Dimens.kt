package main.werewolfkotlin.ui.design

import android.content.Context
import android.view.View

/** Density independent pixels to pixels */
fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun View.dp(value: Int): Int = context.dp(value)
