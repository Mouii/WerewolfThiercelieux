package model

class Hunter(
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
    characterMode: String
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
    characterMode
) {

    override fun clone() = Hunter(this.name, this.description, this.action, this.isSolo, this.isNocturnal, this.powerState, this.condition, this.isWerewolf, this.rolesToStick, this.order, this.maxOccurrence, this.mode)
}