package Model

class Defender(order: Int)
    : Character("The defender can protect from the werewolves attack at night only. He/she must choose a different player each night and can select himself/herself. Doesn't protect from infection, killing potion."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose one player to protect from the attack of werewolves. He/she can protect himself/herself. Must choose a different player every night."
    }

    override fun clone() = Defender(this.order)
}