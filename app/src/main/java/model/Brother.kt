package model

class Brother: Character {

    constructor(order: Int) :
            super("The brothers wake up at night and know each others. This can be a big impact about trust. They can communicate at night with signs."
            , "The Brother recognize each others and can communicate through signs together."
            , true
            , PowerState.PERMANENT
            , false
            , order
            , 3
            , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = Brother(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}