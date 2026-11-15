package model

/***
 * Same kind of role = same class, same mode, same name
 */
fun kindOf(c: CharacterGame) = Triple(c.className, c.mode, c.name)

/***
 * Key written in "rolesToStick" for a role : CLASS-MODE in upper case
 */
fun stickKeyOf(c: CharacterGame): String = stickKey(c.className, c.mode)

fun stickKey(className: String, mode: String): String = (className + "-" + mode).uppercase()
