package Model

class Thief(order: Int)
    : Character("The Thief can select a card from the two the game master show."
    , true
    , PowerState.UNIQUE
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose one of the cards shown."
    }

    override fun clone() = Thief(this.order)
}