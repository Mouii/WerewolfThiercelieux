package Model

class Angel(order: Int)
    : Character("The Angel is a solo role at the beginning. He/she must die on the first turn (night or day) in order to win alone. If he/she survives, then he/she is a simple villager. Putting an Angel makes the game start with the day and not the night (or night with scouting and no actions)."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = Angel(this.order)
}