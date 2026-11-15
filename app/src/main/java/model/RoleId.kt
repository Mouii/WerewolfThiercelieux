package model

/***
 * Names of the roles the rules of the game mention by name.
 *
 * A role is identified by this name everywhere : in the roles file, in a saved game,
 * in the turn order and in the "rolesToStick" links. It is a plain text, never a class name,
 * so renaming or minifying the code can never change the identity of a role.
 */
object RoleId {
    const val WEREWOLF = "Werewolf"
    const val BIG_BAD_WOLF = "BigBadWolf"
    const val WHITE_WEREWOLF = "WhiteWerewolf"
    const val WOLF_FATHER = "WolfFather"
    const val WILD_CHILD = "WildChild"
}
