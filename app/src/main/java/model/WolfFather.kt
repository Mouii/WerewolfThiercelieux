package model

class WolfFather: CharacterGame {

    constructor(order: Int)
    : super("The Wolf Father has the power to convert a victim into a werewolf. One time per game, the Wolf Father can give a sign to the animator to infect the victim. The victim will not be eliminated and will wake up with the others on the werewolves turn."
    , "The WolfFather can infect a victim in order to add one more werewolf to the pack instead of killing it."
        , true
    , PowerState.PERMANENT
    , true
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = WolfFather(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}