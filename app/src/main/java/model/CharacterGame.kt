package model

enum class PowerState {
    PERMANENT,
    CONSUMABLE,
    CONDITIONAL,
    UNIQUE
}

enum class CharacterMode {
    NORMAL, AUTHOR
}

open class CharacterGame(
    var description: String,
    var action: String,
    var isNocturnal: Boolean,
    var powerState: PowerState,
    var isWerewolf: Boolean,
    var order: Int,
    var maxOccurrence: Int,
    var mode: String
) : Cloneable {

    val className: String = this.javaClass.simpleName

    open fun action(): String {
        if(powerState == PowerState.UNIQUE)
            isNocturnal = false

        return action
    }

    public override fun clone() = CharacterGame(this.description
        , this.action
        , this.isNocturnal
        , this.powerState
        , this.isWerewolf
        , this.order
        , this.maxOccurrence
        , this.mode)
}