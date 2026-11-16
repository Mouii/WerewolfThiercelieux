package model

/***
 * Kind of power a role has
 */
enum class PowerState {
    /** Always active. Also used for a role that has no power */
    PERMANENT,

    /** Can be used once at any moment of the game, the moderator decides when */
    CONSUMABLE,

    /** Starts to work when the condition of the role is met (see ConditionalActivation) */
    CONDITIONAL,

    /** Played when the game is set up (thief, cupid...) : the role is not called again afterwards */
    SETUP
}
