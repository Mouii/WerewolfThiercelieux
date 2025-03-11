package Model

class Fox(order: Int)
    : Character("The Fox can scan three players at the same time and see if there is a werewolf among them. If there is at least one, the Fox keeps its power. Otherwise he lost it. The Fox can refuse to use its power to use it later in the game."
    , true
    , PowerState.VARIABLE
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className CAN choose a person to scan with the two neighbours of the target in order to know if there is werewolf among them. If there is none, he lost his power."
    }

    override fun clone() = Fox(this.order)
}