package model

/***
 * A role of the game, as it is described in the roles file, and as it is given to a player.
 *
 * There is only one class for every role : a role is described by its data, not by its own class.
 * [className] is the identity of the role ("Seer", "Werewolf"...), see [RoleId].
 */
open class CharacterGame(
    //Identity of the role. Two roles with the same className are the same role
    val className: String,
    var name: String,
    var description: String,
    var action: String,
    var powerState: PowerState,
    var isNocturnal: Boolean,
    var isWerewolf: Boolean,
    val isSolo: Boolean,
    var maxOccurrence: Int,
    var condition: ConditionalActivation,
    var rolesToStick: Array<String>,
    //Does the role choose players ? If not, it has no block of targets and no lights
    var doTargeting: Boolean,
    //Does the target of the role die ? If it does, the target is a death. If not, it is a simple targeting
    var doKilling: Boolean,
    var mode: String
) : Cloneable {

    //Moment of the role in the night. It is not a characteristic of the role : the game gives it to each role
    //given to a player, from the usual order of the roles (see RoleSequence) and the links with the other roles
    var order: Int = 0

    //Infected by the infect father of wolves : plays with the werewolves but keeps its own role
    var isInfected: Boolean = false

    //Night state of the role when it was created. Useful to trigger a change of status from this state
    val originalNightState: Boolean = isNocturnal

    /***
     * The role is called : text of its action.
     * A power used only once stops waking up its player, so the role is not called again.
     */
    fun nightAction(): String {
        //Setup state is one awakening then go to sleep
        val usedUp = powerState == PowerState.SETUP

        val conditionOver = !isWerewolf
                && powerState == PowerState.CONDITIONAL
                && condition != ConditionalActivation.IS_ALONE

        //The big bad wolf eats with the pack and then once alone : it stays awake after its action
        val staysAwake = className == RoleId.BIG_BAD_WOLF

        if ((usedUp || conditionOver) && !staysAwake) isNocturnal = false

        return action
    }

    /***
     * Independent copy of the role : the player can change his own copy (night state, power used...)
     * without touching the role of the roles file.
     */
    public override fun clone(): CharacterGame {
        val copy = CharacterGame(
            className, name, description, action, powerState, isNocturnal, isWerewolf, isSolo,
            maxOccurrence, condition, rolesToStick.copyOf(), doTargeting, doKilling, mode
        )
        copy.order = order
        copy.isInfected = isInfected
        return copy
    }

    /***
     * Copy of the role for a player, placed at this moment of the night.
     * The copy starts clean : a role given to a player is never infected yet.
     */
    fun copyAt(order: Int): CharacterGame {
        val copy = clone()
        copy.order = order
        copy.isInfected = false
        return copy
    }
}
