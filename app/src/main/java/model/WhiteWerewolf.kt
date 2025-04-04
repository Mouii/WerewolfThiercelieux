package model

class WhiteWerewolf: CharacterGame {

    constructor(order: Int)
    : super("The white werewolf is a solo role. He/she wins when he/she is the last one alive. In order to do it, one night on two, he/she can wake up alone and eat one of his/her werewolves companions. He/she still eats villagers like the others in the common turn."
    , "The WhiteWerewolf, choose a victim to eat with the pack. On his/her own turn, he/she can eat a werewolf."
        , true
    , PowerState.PERMANENT
    , true
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = WhiteWerewolf(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}