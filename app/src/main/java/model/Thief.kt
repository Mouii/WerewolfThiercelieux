package model

class Thief(
    name: String,
    description: String,
    action: String,
    powerState: PowerState,
    isNocturnal: Boolean,
    isWerewolf: Boolean,
    isSolo: Boolean,
    maxOccurrence: Int,
    condition: ConditionalActivation,
    rolesToStick: Array<String>,
    doTargeting: Boolean,
    doKilling: Boolean,
    mode: String
) : CharacterGame(
    name,
    description,
    action,
    powerState,
    isNocturnal,
    isWerewolf,
    isSolo,
    maxOccurrence,
    condition,
    rolesToStick,
    doTargeting,
    doKilling,
    mode
) {

    override fun clone() = Thief(name, description, action, powerState, isNocturnal, isWerewolf, isSolo, maxOccurrence, condition, rolesToStick, doTargeting, doKilling, mode).also { it.order = order }
}