package model

class Servant: CharacterGame {
    constructor(order: Int)
            : super("The Servant can stop the execution of the selected victim during the day and take his/her place. To do it, the Servant has to reveal himself/herself and take the role of the selected player. The eliminated player stay eliminated however."
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

    override fun clone() = Servant(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}