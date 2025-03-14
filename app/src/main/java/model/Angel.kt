package model

class Angel: Character {

    constructor(order: Int) :
    super("The Angel is a solo role at the beginning. He/she must die on the first turn (night or day) in order to win alone. If he/she survives, then he/she is a simple villager. Putting an Angel makes the game start with the day and not the night (or night with scouting and no actions)."
        , "No action for night"
        , false
        , PowerState.PERMANENT
        , false
        , order
        , 1
        , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState,
                isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Angel(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}