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
    ON_MORNING,

}

open class CharacterGame(
    var name : String,
    var description: String,
    var action: String,
    val isSolo: Boolean,
    var isNocturnal: Boolean,
    var powerState: PowerState,
    var condition: ConditionalActivation,
    var isWerewolf: Boolean,
    var rolesToStick: Array<String>,
    var order: Int,
    var maxOccurrence: Int,
    var mode: String
) : Cloneable {

    //Does the role allow its player to target for non-killing purpose?
    var doTargeting: Boolean = false

    //Does the role allow to kill another player?
    var doKilling: Boolean = false

    //Infected by the grandfather of wolves : plays with the werewolves but keeps its own role
    var isInfected: Boolean = false

    //Keep the classname for different purposes
    val className: String = this.javaClass.simpleName

    //Memorize the
    val originalNightState : Boolean = isNocturnal

    open fun action(): String {
        //In unique statement, call one time then over
        //In conditional, call one time then over, then handled in the game activity to start again
        if(powerState == PowerState.UNIQUE || (!isWerewolf && powerState == PowerState.CONDITIONAL
                                                && condition != ConditionalActivation.IS_ALONE))
            isNocturnal = false

        return action
    }

    public override fun clone() = CharacterGame(this.name
        , this.description
        , this.action
        , this.isNocturnal
        , this.isSolo
        , this.powerState
        , this.condition
        , this.isWerewolf
        , this.rolesToStick
        , this.order
        , this.maxOccurrence
        , this.mode)
}