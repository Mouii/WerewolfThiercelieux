package model

class RustyKnight: Character {
    constructor(order: Int)
            : super("The RustyKnight, if killed during the night, can kill a werewolf from tetanus. The next morning, the alive werewolf the most at the left of the knight will die. This action helps a lot to see innocents between the knight and the killed werewolf."
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

    override fun clone() = RustyKnight(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}