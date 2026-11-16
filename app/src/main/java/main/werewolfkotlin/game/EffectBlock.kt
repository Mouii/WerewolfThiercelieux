package main.werewolfkotlin.game

/***
 * Block of the "Roles" tab : a named list of players linked to the effect of a role.
 * The players can only be added by selecting them.
 * - TARGETS : the players chosen by the role
 * - INFECTION : the players infected by the infect father of wolves
 * - DEATHS : the dead players, with a button to clear them quickly
 * - MODEL : the player whose role was copied by the role (the doppleganger). Never emptied
 */
class EffectBlock(
    var title: String,
    val kind: Kind = Kind.TARGETS,
    val roleKey: Triple<String, String, String>? = null   //Role who owns the block, see model.kindOf()
) {
    enum class Kind { TARGETS, INFECTION, DEATHS, MODEL }

    val players: MutableList<Seat> = mutableListOf()

    /** Red light on the targets during the turn of the role, green light on the role during their turn */
    var lights: Boolean = (kind == Kind.TARGETS || kind == Kind.MODEL) && roleKey != null
}
