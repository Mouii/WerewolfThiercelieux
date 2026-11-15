package main.werewolfkotlin.data

import android.content.Context

/***
 * Allow getting a string resource by a string name. Empty if there is no such string.
 */
fun Context.stringByKey(key: String): String {
    val idRes = resources.getIdentifier(key, "string", packageName)
    return if (idRes != 0) getString(idRes) else ""
}
