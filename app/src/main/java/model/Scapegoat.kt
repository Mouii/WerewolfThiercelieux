package model

class Scapegoat: Character {
    constructor(order: Int)
            : super("The scapegoat is eliminated if there is a tie in the votes during the day. He/she may choose who can vote on the next day. This can be dangerous if the remaining voters are werewolves. It's also dangerous if there is only one person allowed to vote and is killed during the night; leading to no vote."
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

    override fun clone() = Scapegoat(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}