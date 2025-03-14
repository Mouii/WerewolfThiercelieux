package model

class Crow: Character {

    constructor(order: Int)
    : super("The Crow can give two votes to a player that will apply during the day."
    , "The Crow choose a player to get two votes for the day to come."
        , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Crow(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}