package model

enum class PowerState {
    PERMANENT,
    CONSUMABLE,
    CONDITIONAL,
    UNIQUE
}

enum class ConditionalActivation {
    NOCONDITION,
    ONEWEREWOLF,
    ONEVILLAGER,
    ALLWEREWOLVES,
    ALLVILLAGERS,
    LINKEDROLES,
    ISALONE
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

    //Keep the classname for different purposes
    val className: String = this.javaClass.simpleName

    //Useful to trigger chance of status from the original night state
    val originalNightState = if(powerState != PowerState.CONDITIONAL)
        null
    else
        isNocturnal

    open fun action(): String {
        //In unique statement, call one time then over
        //In conditional, call one time then over, then handled in the game activity to start again
        if(powerState == PowerState.UNIQUE || (!isWerewolf && powerState == PowerState.CONDITIONAL
                                                && condition != ConditionalActivation.ISALONE))
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