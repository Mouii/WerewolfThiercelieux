package model

enum class PowerState {
    PERMANENT, VARIABLE, UNIQUE
}

enum class CharacterMode {
    NORMAL, AUTHOR
}

open class Character(
    var description: String,
    var action: String,
    var isNocturnal: Boolean,
    var powerState: PowerState,
    var isWerewolf: Boolean,
    var order: Int,
    var maxOccurrence: Int,
    var mode: CharacterMode
) : Cloneable {

    val className: String = this.javaClass.simpleName

    open fun action(): String {
        return action
    }

    public override fun clone() = Character(this.description
        , this.action
        , this.isNocturnal
        , this.powerState
        , this.isWerewolf
        , this.order
        , this.maxOccurrence
        , this.mode)
}