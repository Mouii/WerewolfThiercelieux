package model

class BearTamer:CharacterGame {

    constructor(order: Int) :
    super("The BearTamer can signal if one of his neighbours is a werewolf. If the game master makes a sound like a bear growl, that means one of the alive neighbours of the BearTamer is a werewolf. The Bear also growls if the BearTamer is infected."
        , "No action for night"
        , false
        , PowerState.PERMANENT
        , false
        , order
        , 1
        , "NORMAL")

    constructor(description: String, action: String, isNocturnal: Boolean, powerState: PowerState
                , isWerewolf: Boolean, order : Int, maxOccurrence : Int, characterMode: String) :
    super(description, action, isNocturnal, powerState, isWerewolf, order, maxOccurrence, characterMode)

    override fun clone() = BearTamer(this.description, this.action, this.isNocturnal, this.powerState, this.isWerewolf, this.order, this.maxOccurrence, this.mode)

}