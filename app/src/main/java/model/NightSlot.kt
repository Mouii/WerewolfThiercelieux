package model

/***
 * When a role is called during the night
 */
enum class NightSlot {
    /** At its own moment of the turn order */
    OWN_MOMENT,

    /** With the pack of werewolves, whenever the pack is called */
    WEREWOLF_TURN
}
