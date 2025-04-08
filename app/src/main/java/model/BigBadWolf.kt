package model

class BigBadWolf : CharacterGame {
    constructor(order: Int)
        : super("The big bad wolf can eat a second victim during the night. He/she eats with the others on the common turn then has a unique turn for him/her. This power is lost when a werewolf, wild child, or wolfhound is dead."
    , "The BigBadWolf choose a victim to eat. On its only turn, he can eat another victim"
    , true
    , PowerState.PERMANENT
    , true
    , order
    , 1
    , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = BigBadWolf(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}