package main.werewolfkotlin.ui.design

import android.content.Context
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/***
 * The only way the code gets a colour : by its name in res/values/colors.xml.
 * No colour value is written in the Kotlin code.
 */
fun Context.colorOf(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun View.colorOf(@ColorRes id: Int): Int = context.colorOf(id)
