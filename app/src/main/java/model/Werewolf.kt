package model

class Werewolf: CharacterGame {

    constructor(order: Int)
    : super("The werewolf can select a victim during the night to eat and eliminate from the game."
    , "The Werewolf choose a victim to eat."
        , true
    , PowerState.PERMANENT
    , true
    , order
    , 6
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Werewolf(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}