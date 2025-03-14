package model

class Villager: Character {

    constructor(order: Int)
    : super("The villager has no other power than its own deduction to find out the werewolves."
    , "No action during the night"
        , false
    , PowerState.PERMANENT
    , false
    , order
    , 6
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Villager(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}