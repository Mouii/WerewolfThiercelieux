package Model

class WildChild(order: Int)
    : Character("The Wild Child choose a player that he/she consider as his/her model. If the model dies, the Wild Child become a werewolf. Otherwise, he's a simple villager."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        isNocturnal = false
        return "The $className choose a player to be his/her master. If the master dies, he/she become a werewolf."
    }

    override fun clone() = WildChild(this.order)
}