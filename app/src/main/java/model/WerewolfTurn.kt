package model

/** className of the "werewolf turn" marker */
const val WOLF_TURN = "WerewolfTurn"

/***
 * Not a player : a marker for the pack of werewolves in the turn order
 */
class WerewolfTurn(
    name: String,
    description: String,
    order: Int
) : CharacterGame(
    name,
    description,
    "",
    PowerState.PERMANENT,
    true,
    false,
    false,
    1,
    ConditionalActivation.NO_CONDITION,
    arrayOf(),
    false,
    false,
    "NORMAL"
) {
    init {
        this.order = order
    }
}
