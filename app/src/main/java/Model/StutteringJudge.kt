package Model

class StutteringJudge(order: Int)
    : Character("The StutteringJudge can execute a second vote during a day immediately after the first. He must give a sign to the game master and do it during the day in order for the animator to proceed to a second vote."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = StutteringJudge(this.order)
}