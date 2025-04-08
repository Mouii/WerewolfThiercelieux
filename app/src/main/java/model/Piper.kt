package model

class Piper: CharacterGame {
    constructor(order: Int)
    : super("The piper is a solo role. He/she has to charm all the characters in the village. The charmed will open their eyes, new and old, every night; which will add more difficulties to the piper to stay hidden. Once he manager to charm everybody, no matter when, he wins. "
    , "The Piper choose two players to charm. Then all the charmed will open their eyes and see who is left."
        , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Piper(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}