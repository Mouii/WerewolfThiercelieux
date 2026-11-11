package model

class PowerfulMotherOfWolves(
    name: String,
    description: String,
    action: String,
    isSolo: Boolean,
    isNocturnal: Boolean,
    powerState: PowerState,
    condition: ConditionalActivation,
    isWerewolf: Boolean,
    rolesToStick: Array<String>,
    order: Int,
    maxOccurrence: Int,
    mode: String
) : CharacterGame(
    name,
    description,
    action,
    isSolo,
    isNocturnal,
    powerState,
    condition,
    isWerewolf,
    rolesToStick,
    order,
    maxOccurrence,
    mode
) {
    override fun clone() = PowerfulMotherOfWolves(name, description, action, isSolo, isNocturnal, powerState, condition, isWerewolf, rolesToStick, order, maxOccurrence, mode)
}