package model

class Fireman: CharacterGame {

    constructor(order: Int)
    : super("The Fireman, when he/she dies, kills the two closed alive neighbours."
    , "No action during the night"
        , false
    , PowerState.CONDITIONAL
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Fireman(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}