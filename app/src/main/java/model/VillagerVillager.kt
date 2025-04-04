package model

class VillagerVillager: CharacterGame {

    constructor(order: Int)
    : super("The villager is someone who everybody knows. He/she can't hide his/her role."
    , "No action during the night"
        , false
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = VillagerVillager(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}