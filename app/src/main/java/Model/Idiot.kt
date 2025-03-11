package Model

class Idiot(order: Int)
    : Character("The Idiot of the village has a second chance if he/she's voted to be killed during the day. Considered as not possible dangerous, he/she stay alive revealed. However, he/she lost the possibility of voting. If there is an Elder in your game and the Elder remove the powers, the idiot die."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = Idiot(this.order)
}