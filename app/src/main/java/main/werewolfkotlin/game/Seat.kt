package main.werewolfkotlin.game

import model.CharacterGame

/***
 * One drawn player around the table. The role stays null until the moderator assigns it.
 */
class Seat(
    var name: String,
    var character: CharacterGame? = null,
    var alive: Boolean = true,
    var note: String = ""
)
