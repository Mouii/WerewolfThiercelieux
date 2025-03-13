package Model

class WhiteWerewolf(order: Int)
    : Character("The white werewolf is a solo role. He/she wins when he/she is the last one alive. In order to do it, one night on two, he/she can wake up alone and eat one of his/her werewolves companions. He/she still eats villagers like the others in the common turn."
    , true
    , PowerState.PERMANENT
    , true
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose a victim to eat with the pack. On his/her own turn, he/she can eat a werewolf."
    }

    override fun clone() = WhiteWerewolf(this.order)
}