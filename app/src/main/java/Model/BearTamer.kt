package Model

class BearTamer(order: Int)
    : Character("The BearTamer can signal if one of his neighbours is a werewolf. If the game master makes a sound like a bear growl, that means one of the alive neighbours of the BearTamer is a werewolf. The Bear also growls if the BearTamer is infected."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = BearTamer(this.order)
}