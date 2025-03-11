package Model

class Villager(order: Int)
    : Character("The villager has no other power than its own deduction to find out the werewolves."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 6) {

    override fun clone() = Villager(this.order)
}