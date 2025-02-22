package Model

class Witch(order: Int, imageResource: Int)
    : Character("The Witch has two potions; one of dead and one of resurrection, that she can use in the same time."
    , true
    , PowerState.VARIABLE
    , false
    , order
    , imageResource) {

    override fun action(): String {
        return "The $className can use her potion of life to save the victim, and use her potion of death to kill someone."
    }
}