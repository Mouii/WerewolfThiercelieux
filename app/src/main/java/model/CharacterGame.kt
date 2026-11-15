package model

enum class PowerState {
    PERMANENT,
    CONSUMABLE,
    CONDITIONAL,
    UNIQUE
}

enum class ConditionalActivation {
    NO_CONDITION,
    ONE_WEREWOLF,
    ONE_VILLAGER,
    ALL_WEREWOLVES,
    ALL_VILLAGERS,
    LINKED_PLAYERS,
    IS_ALONE,
    ON_MORNING
}

open class CharacterGame(
    var name : String,
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
    //given to a player, from the usual order of the roles (see RoleOrder) and the links with the other roles
    var order: Int = 0

    //Infected by the infect father of wolves : plays with the werewolves but keeps its own role
    var isInfected: Boolean = false

    //Keep the classname for different purposes
    val className: String = this.javaClass.simpleName

    //Night state of the role when it was created. Useful to trigger a change of status from this state
    val originalNightState: Boolean = isNocturnal

    open fun action(): String {
        //In unique statement, call one time then over
        //In conditional, call one time then over, then handled in the game activity to start again
        if(powerState == PowerState.UNIQUE || (!isWerewolf && powerState == PowerState.CONDITIONAL
                                                && condition != ConditionalActivation.IS_ALONE))
            isNocturnal = false

        return action
    }

    public override fun clone(): CharacterGame {
        val copy = CharacterGame(name, description, action, powerState, isNocturnal, isWerewolf, isSolo,
            maxOccurrence, condition, rolesToStick, doTargeting, doKilling, mode)
        copy.order = order
        return copy
    }
}
