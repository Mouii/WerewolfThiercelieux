package model

class VillagerVillager(
    description: String,
    action: String,
    isNocturnal: Boolean,
    powerState: PowerState,
    isWerewolf: Boolean,
    order: Int,
    maxOccurrence: Int,
    characterMode: String
) : CharacterGame(
    description,
    action,
    isNocturnal,
    powerState,
    isWerewolf,
    order,
    maxOccurrence,
    characterMode
) {

    override fun clone() = VillagerVillager(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}