package model

class Actor : Character {

    constructor(order: Int):
    super("The actor can play three different roles during the game at night. These roles must be choose outside of the game and are visible to everybody. Once the three have been played, the Actor become a simple villager"
        ,"The Actor might choose one of the roles visible to play for a complete turn. Once the role has been played, it is discarded."
        , true
        , PowerState.CONSUMABLE
        , false
        , order
        , 1
        , CharacterMode.NORMAL)

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState,
                isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: CharacterMode) :
            super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun action(): String {

        //To better handle after. For now, in author version, not called anymore
        if(mode == CharacterMode.AUTHOR)
            isNocturnal = false

        return action
    }

    override fun clone() = Actor(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)
}