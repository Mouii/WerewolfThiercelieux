package model

class WolfHound: Character {

    constructor(order: Int)
    : super("The WolfHound is a role that can choose who he/she wants to be between a werewolf and a simple villager."
    , "The WolfHound choose to wake up and be a werewolf for the game or stay asleep and be a simple villager."
        , false
    , PowerState.CONSUMABLE
    , true
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = WolfHound(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}