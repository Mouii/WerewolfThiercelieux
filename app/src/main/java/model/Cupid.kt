package model

class Cupid: CharacterGame {

    constructor (order: Int)
    : super("Cupid is called only on the first night to unite a couple."
    , "The Cupid choose two people to put in love together. If one dies, so die the other."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun action(): String {
        isNocturnal = false
        return action
    }

    override fun clone() = Cupid(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}