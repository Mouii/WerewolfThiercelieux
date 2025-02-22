package Model

class Werewolf(order: Int, imageResource: Int)
    : Character("The werewolf can select a victim during the night to eat and eliminate from the game."
    , true
    , PowerState.PERMANENT
    , true
    , order
    , imageResource) {

    override fun action(): String {
        return "The $className choose a victim to eat."
    }
}