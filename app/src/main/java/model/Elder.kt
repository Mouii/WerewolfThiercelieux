package model

class Elder: CharacterGame {
    constructor(order: Int)
    : super("Elder can survive a second attack of werewolves. If the village eliminate him/her, they don't have anymore powers. Doesn't apply from being killed by love. Can't be infected. If he/she gets a heal potion, he/she gets one more life."
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

    override fun clone() = Elder(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}