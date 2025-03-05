package Model

class Seer(order: Int)
    : Character("The Seer can see the card of another player one time per turn."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose a card to see."
    }

    override fun clone() = Seer(this.order)
}