package model

class Defender:Character {

    constructor(order: Int)
    : super("The defender can protect from the werewolves attack at night only. He/she must choose a different player each night and can select himself/herself. Doesn't protect from infection, killing potion."
    , "The Defender choose one player to protect from the attack of werewolves. He/she can protect himself/herself. Must choose a different player every night."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1
    , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)


    override fun clone() = Defender(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}