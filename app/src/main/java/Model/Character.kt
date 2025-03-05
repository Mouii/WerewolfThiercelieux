package Model

enum class PowerState {
    PERMANENT, VARIABLE, UNIQUE
}

open class Character(
    val description: String,
    var isNocturnal: Boolean,
    val powerState: PowerState,
    var isWerewolf: Boolean,
    var order: Int,
    val maxOccurence: Int
) : Cloneable {

    val className: String = this.javaClass.simpleName

    open fun action(): String {
        return "$className does nothing special."
    }

    public override fun clone() = Character(this.description,
        this.isNocturnal,
        this.powerState,
        this.isWerewolf,
        this.order,
        this.maxOccurence)
}