package Model

class LittleGirl(order: Int)
    : Character("The Little Girl can try to spy the werewolves; but if she's detected, she's dead."
    , true
    , PowerState.PERMANENT
    , true //In order to be called during their turn
    , order
    , 1) {

    override fun action(): String {
        return "The $className spy on the werewolves turn."
    }

    override fun clone() = LittleGirl(this.order)
}