package Model

class WolfHound(order: Int)
    : Character("The WolfHound is a role that can choose who he/she wants to be between a werewolf and a simple villager."
    , true
    , PowerState.PERMANENT
    , true
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose to wake up and be a werewolf for the game or stay asleep and be a simple villager."
    }

    override fun clone() = WolfHound(this.order)
}