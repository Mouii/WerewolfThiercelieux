package model

class WildChild: Character {

    constructor(order: Int)
    : super("The Wild Child choose a player that he/she consider as his/her model. If the model dies, the Wild Child become a werewolf. Otherwise, he's a simple villager."
    , "The WildChild choose a player to be his/her master. If the master dies, he/she become a werewolf."
        , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = WildChild(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}