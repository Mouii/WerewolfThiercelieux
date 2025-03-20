package model

class Witch: Character {

    constructor(order: Int)
    : super("The Witch has two potions; one of dead and one of resurrection, that she can use in the same time."
    , "The Witch can use her potion of life to save the victim, and use her potion of death to kill someone."
        , true
    , PowerState.VARIABLE
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Witch(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}