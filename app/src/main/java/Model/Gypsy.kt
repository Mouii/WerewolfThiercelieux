package Model

class Gypsy(order: Int)
    : Character("The Gypsy can make players ask questions to the deaths in order to have more information. On his/her turn, he/she choose a card and a player to read it. On the morning, the chosen player will read it. If the chosen player must die, the card isn't read."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose a card between the spiritism cards and a player to read it."
    }

    override fun clone() = Gypsy(this.order)
}