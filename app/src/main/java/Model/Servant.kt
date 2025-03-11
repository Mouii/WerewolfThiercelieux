package Model

class Servant(order: Int)
    : Character("The Servant can stop the execution of the selected victim during the day and take his/her place. To do it, the Servant has to reveal himself/herself and take the role of the selected player. The eliminated player stay eliminated however."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = Servant(this.order)
}