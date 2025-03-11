package Model

class Scapegoat(order: Int)
    : Character("The scapegoat is eliminated if there is a tie in the votes during the day. He/she may choose who can vote on the next day. This can be dangerous if the remaining voters are werewolves. It's also dangerous if there is only one person allowed to vote and is killed during the night; leading to no vote."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = Scapegoat(this.order)
}