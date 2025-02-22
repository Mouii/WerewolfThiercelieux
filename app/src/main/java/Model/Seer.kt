package Model

class Seer(order: Int, imageResource: Int)
    : Character("The Seer can see the card of another player one time per turn."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , imageResource) {

    override fun action(): String {
        return "The $className choose a card to see."
    }
}