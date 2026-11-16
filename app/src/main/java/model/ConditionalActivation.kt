package model

/***
 * What has to happen for the power of a CONDITIONAL role to change (see RoleChange)
 */
enum class ConditionalActivation {
    /** Nothing : the role never changes by itself */
    NO_CONDITION,

    /** A player of the werewolf side died */
    ONE_WEREWOLF,

    /** A player of the village side died */
    ONE_VILLAGER,

    /** Every player of the werewolf side is dead */
    ALL_WEREWOLVES,

    /** Every player of the village side is dead */
    ALL_VILLAGERS,

    /** This role is the last one of its kind in the game (sisters, brothers) */
    IS_ALONE,

    /** A player this role chose is dead (the mentor of the wild child) */
    MY_TARGET_DIES,

    /** This role has just chosen a player (the doppleganger picks who it copies) */
    TARGET_CHOSEN,

    /** The game reaches the turn written in the role (the mother of wolves after two nights) */
    TURN_REACHED,

    /** The morning comes */
    ON_MORNING
}
