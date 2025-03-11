package Model

class Piper(order: Int)
    : Character("The piper is a solo role. He/she has to charm all the characters in the village. The charmed will open their eyes, new and old, every night; which will add more difficulties to the piper to stay hidden. Once he manager to charm everybody, no matter when, he wins. "
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose two players to charm. Then all the charmed will open their eyes and see who is left."
    }

    override fun clone() = Piper(this.order)
}