package model

/***
 * The only role name the code still needs.
 *
 * Everything else a role does is described in the roles file : see CharacterGame for the fields
 * that replaced the old special cases (playsAt, hasSeparateTurn, ownsInfectionBlock,
 * definesPackTurn, becomes).
 */
object RoleId {
    /***
     * The place of the pack of werewolves in the turn order is remembered under this name,
     * because the pack is not a role : it has no place of its own to be saved under.
     */
    const val WEREWOLF = "Werewolf"
}
