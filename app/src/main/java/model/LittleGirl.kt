package model

class LittleGirl: CharacterGame {
    constructor(order: Int)
            : super("The Little Girl can try to spy the werewolves; but if she's detected, she's dead."
        , "The LittleGirl spy on the werewolves turn."
        , true
        , PowerState.PERMANENT
        , true //In order to be called during their turn
        , order
        , 1
        , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)


    override fun clone() = LittleGirl(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}