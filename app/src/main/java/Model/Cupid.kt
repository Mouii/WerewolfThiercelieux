package Model

class Cupid(order: Int)
    : Character("Cupid is called only on the first night to unite a couple."
    , true
    , PowerState.VARIABLE
    , false
    , order
    , 1) {

    override fun action(): String {
        isNocturnal = false
        return "The $className choose two people to put in love together. If one dies, so die the other."
    }

    override fun clone() = Cupid(this.order)
}