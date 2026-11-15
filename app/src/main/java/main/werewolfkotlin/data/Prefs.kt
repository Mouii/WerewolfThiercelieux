package main.werewolfkotlin.data

import android.content.Context
import android.content.SharedPreferences

/***
 * Every SharedPreferences file of the application, with its name in one place
 */
object Prefs {
    const val APP = "AppPreferences"            //Language of the application
    const val PLAYER_NAMES = "PlayerNames"      //Names and number of players of the game setup
    const val GAME_SAVE = "GameSave"            //Game in progress
    const val EXTENSIONS = "Extensions"         //Activated extensions
    const val ROLE_ORDER = "RoleOrder"          //Moment of the night of each role
    const val INFORMATION = "informations"      //Free notes of the moderator
}

fun Context.prefs(name: String): SharedPreferences = getSharedPreferences(name, Context.MODE_PRIVATE)
