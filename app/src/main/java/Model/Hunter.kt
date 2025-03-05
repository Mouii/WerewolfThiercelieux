package Model

class Hunter(order: Int)
    : Character("The Hunter can, when he dies, kill someone else alive."
    , false
    , PowerState.UNIQUE
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className doesn't have any action in night"
    }

    override fun clone() = Hunter(this.order)
}