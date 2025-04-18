package model

import main.werewolfkotlin.R
import main.werewolfkotlin.WorkerEasier

enum class PowerState(val value: String) {
    PERMANENT(WorkerEasier.getString(R.string.Power_Permanent)),
    CONSUMABLE(WorkerEasier.getString(R.string.Power_Consumable)),
    CONDITIONAL(WorkerEasier.getString(R.string.Power_Conditional)),
    UNIQUE(WorkerEasier.getString(R.string.Power_Unique))
}

enum class ConditionalActivation(val value: String)  {
    NOCONDITION(WorkerEasier.getString(R.string.Condition_NoCondition)),
    ONEWEREWOLF(WorkerEasier.getString(R.string.Condition_OneWerewolf)),
    ONEVILLAGER(WorkerEasier.getString(R.string.Condition_OneVillager)),
    ALLWEREWOLVES(WorkerEasier.getString(R.string.Condition_AllWerewolves)),
    ALLVILLAGERS(WorkerEasier.getString(R.string.Condition_AllVillagers)),
    LINKEDROLES(WorkerEasier.getString(R.string.Condition_LinkedRoles)),
    ISALONE(WorkerEasier.getString(R.string.Condition_IsAlone))
}

open class CharacterGame(
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

    //Name adapted to language
    val name : String = WorkerEasier.getStringByKey(className)

    //Useful to trigger chance of status from the original night state
    val originalNightState = if(powerState != PowerState.CONDITIONAL)
        null
    else
        isNocturnal

    open fun action(): String {
        if(powerState == PowerState.UNIQUE)
            isNocturnal = false

        return action
    }

    public override fun clone() = CharacterGame(this.description
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