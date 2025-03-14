package model

class Manipulator: Character {

    constructor(order: Int)
    : super("The Manipulator is a solo role where he has to win with only a part of the village. Once the other part is eliminated, he wins."
    , "The Manipulator wakes up and see with who is part of his group. He must eliminate all the others. Once they are dead, he wins."
        , true
    , PowerState.VARIABLE
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun action(): String {

        //Must become a killer if no werewolves alive on AUTHOR version
        if(mode == CharacterMode.NORMAL)
            isNocturnal = false

        return action
    }

    override fun clone() = Manipulator(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}