package Model

class Elder(order: Int)
    : Character("Elder can survive a second attack of werewolves. If the village eliminate him/her, they don't have anymore powers. Doesn't apply from being killed by love. Can't be infected. If he/she gets a heal potion, he/she gets one more life."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = Elder(this.order)
}