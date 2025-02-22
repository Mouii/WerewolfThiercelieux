package Model

class Hunter(order: Int, imageResource: Int)
    : Character("The Hunter can, when he dies, kill someone else alive."
    , false
    , PowerState.UNIQUE
    , false
    , order
    , imageResource) {

    override fun action(): String {
        return "The $className doesn't have any action in night"
    }
}