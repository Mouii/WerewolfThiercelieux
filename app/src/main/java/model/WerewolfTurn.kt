package model

/** className of the "werewolf turn" marker */
const val WOLF_TURN = "WerewolfTurn"

/***
 * Not a player : a marker placed in the turn order to call the pack of werewolves
 * when no simple werewolf is in the game. isNocturnal tells if the turn is enabled.
 */
class WerewolfTurn(
    name: String,
    description: String,
    order: Int
) : CharacterGame(
    name,
    description,
    "",
    false,
    true,
    PowerState.PERMANENT,
    ConditionalActivation.NOCONDITION,
    false,
    arrayOf(),
    order,
    1,
    "NORMAL"
)
