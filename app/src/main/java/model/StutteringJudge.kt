package model

class StutteringJudge: Character {

    constructor(order: Int)
    : super("The StutteringJudge can execute a second vote during a day immediately after the first. He must give a sign to the game master and do it during the day in order for the animator to proceed to a second vote."
    , "No action during the night"
        , false
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = StutteringJudge(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}