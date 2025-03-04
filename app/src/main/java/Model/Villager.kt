package Model

class Villager(order: Int, imageResource: Int)
    : Character("The villager has no other power than its own deduction to find out the werewolves."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , imageResource) {

    override var maxOccurence: Int = 6

    override fun action(): String {
        return "The $className doesn't have any action in night"
    }
}