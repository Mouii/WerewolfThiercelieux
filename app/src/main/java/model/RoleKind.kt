package model

/***
 * Same kind of role = same role, same variant
 */
fun kindOf(c: CharacterGame) = Triple(c.className, c.source, c.key)
