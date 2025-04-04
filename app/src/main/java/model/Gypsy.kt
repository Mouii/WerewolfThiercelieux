package model

class Gypsy: CharacterGame {
    constructor(order: Int)
    : super("The Gypsy can make players ask questions to the deaths in order to have more information. On his/her turn, he/she choose a card and a player to read it. On the morning, the chosen player will read it. If the chosen player must die, the card isn't read."
    , "The Gypsy choose a card between the spiritism cards and a player to read it."
        , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Gypsy(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}