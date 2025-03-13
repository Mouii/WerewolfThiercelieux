package Model

class RustyKnight(order: Int)
    : Character("The RustyKnight, if killed during the night, can kill a werewolf from tetanus. The next morning, the alive werewolf the most at the left of the knight will die. This action helps a lot to see innocents between the knight and the killed werewolf."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = RustyKnight(this.order)
}