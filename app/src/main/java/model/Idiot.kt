package model

class Idiot: CharacterGame {

    constructor(order: Int)
    : super("The Idiot of the village has a second chance if he/she's voted to be killed during the day. Considered as not possible dangerous, he/she stay alive revealed. However, he/she lost the possibility of voting. If there is an Elder in your game and the Elder remove the powers, the idiot die."
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

    override fun clone() = Idiot(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}