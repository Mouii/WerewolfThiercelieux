package Model

class Manipulator(order: Int)
    : Character("The Manipulator is a solo role where he has to win with only a part of the village. Once the other part is eliminated, he wins."
    , true
    , PowerState.VARIABLE
    , false
    , order
    , 1) {

    override fun action(): String {
        isNocturnal = false
        return "The $className wakes up and see with who is part of his group. He must eliminate all the others. Once they are dead, he wins."
    }

    override fun clone() = Manipulator(this.order)
}