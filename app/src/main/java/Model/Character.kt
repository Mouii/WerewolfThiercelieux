package Model

import androidx.annotation.DrawableRes

enum class PowerState {
    PERMANENT, VARIABLE, UNIQUE
}

open class Character(
    val description: String,
    var isNocturnal: Boolean,
    val powerState: PowerState,
    var isWerewolf: Boolean,
    var order: Int,
    @DrawableRes val imageResource: Int
) {

    val className: String = this.javaClass.simpleName

    

    open fun action(): String {
        return "$className does nothing special."
    }
}