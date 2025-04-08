package model

class Fox: CharacterGame {
    constructor(order: Int)
            : super("The Fox can scan three players at the same time and see if there is a werewolf among them. If there is at least one, the Fox keeps its power. Otherwise he lost it. The Fox can refuse to use its power to use it later in the game."
        , "The Fox CAN choose a person to scan with the two neighbours of the target in order to know if there is werewolf among them. If there is none, he lost his power."
        , true
        , PowerState.CONSUMABLE
        , false
        , order
        , 1
        , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Fox(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}