package main.werewolfkotlin.game

/***
 * Every message the rules of the game can produce.
 *
 * The rules never build a sentence : they say WHAT happened, with the names it concerns.
 * The screen turns it into a sentence of the language of the phone (see ui.Narrator).
 * That way the rules can be tested without Android, and the historic of a game
 * follows the language even if the moderator changes it during the game.
 */
enum class GameMessage {
    PLAYER_DEAD,            //a player died                     : player, role
    PLAYER_ALIVE,           //a player came back to the game    : player, role
    PLAYER_INFECTED,        //a player was infected             : player, role
    PLAYER_DISINFECTED,     //a player is not infected anymore  : player, role
    ELDER_POWER_USED,       //the villagers lost their powers   : no name
    GAME_RESTARTED,         //the game started again            : no name
    ROLES_RESET,            //every role was taken away         : no name
    ROLES_AWAKE,            //roles wake up                     : label of the moment
    ROLES_ASLEEP,           //roles go to sleep                 : label of the moment
    ROLES_AWAKE_PENDING,    //will wake up when the night ends  : label of the moment
    ROLES_ASLEEP_PENDING,   //will sleep when the night ends    : label of the moment
    DEATH_TARGET,           //a victim was chosen               : role, player
    DEATH_TARGET_REMOVED,   //a victim was taken back           : role, player
    TARGET,                 //a player was targeted             : block, player
    TARGET_REMOVED,         //a target was taken back           : block, player
    ROLE_COPIED             //a role took the behaviour of another : role, copied role, player
}

/***
 * A message of the rules : what happened, and the names it concerns, in the order of the message
 */
class GameText(val message: GameMessage, val names: List<String> = emptyList())

/***
 * One line of the historic
 */
class HistoryEntry(val turn: Int, val phase: EnumPhase, val text: GameText)

/***
 * A role is called during the night : what the moderator has to say.
 * The screen puts these pieces together, the rules never do.
 */
class NightCall(
    //One line per role called at this moment : its name and the text of its action
    val roles: List<CalledRole>,
    //Players who follow the pack of werewolves without being werewolves
    val infectedPlayers: List<String> = emptyList()
) {
    class CalledRole(val roleName: String, val action: String)
}

/***
 * The few fixed labels the rules need. The screen gives them at the start of the game,
 * so that no text of the application is written inside the rules.
 */
class GameLabels(
    //Title of the block that holds the players about to die
    val deathsBlock: String,
    //Title of the block that holds the infected players
    val infectedBlock: String,
    //The pack of werewolves when no player holds a werewolf role : its name and what it does
    val packName: String,
    val packAction: String,
    //Name of a player who did not type his name : the number of his seat is given
    val seatName: (seatNumber: Int) -> String
) {
    companion object {
        /** Labels used when no screen gave any : tests, and a badly started game */
        val NONE = GameLabels("Deaths", "Infected", "Werewolves", "The werewolves choose a player to eat.") { number ->
            "Player $number"
        }
    }
}
