package Model

class Elder(order: Int)
    : Character("Elder can survive a second attack of werewolves. If the village eliminate him/her, they don't have anymore powers. Doesn't apply from being killed by love. Can't be infected. If he/she gets a heal potion, he/she gets one more life."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        isNocturnal = false
        return "The $className can survive another attack of the werewolves. If he/she's killed by the village, all special villagers lose their powers. This doesn't apply if killed from love. Can't be infected."
    }

    override fun clone() = Elder(this.order)
}