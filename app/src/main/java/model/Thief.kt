package model

class Thief: CharacterGame {

    constructor(order: Int)
    : super("The Thief can select a card from the two the game master show."
    , "The Thief choose one of the cards shown."
        , true
    , PowerState.CONDITIONAL
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Thief(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}