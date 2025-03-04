package Model

class Cupid(order: Int, imageResource: Int)
    : Character("Cupid is called only on the first night to unite a couple."
    , true
    , PowerState.VARIABLE
    , false
    , order
    , imageResource) {

    override var maxOccurence: Int = 1


    override fun action(): String {
        isNocturnal = false
        return "The $className choose two people to put in love together. If one dies, so die the other."
    }
}