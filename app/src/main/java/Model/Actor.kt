package Model

class Actor(order: Int)
    : Character("The actor can play three different roles during the game at night. These roles must be choose outside of the game and are visible to everybody. Once the three have been played, the Actor become a simple villager"
    , true
    , PowerState.VARIABLE
    , false
    , order
    , 1) {

    override fun action(): String {
        return "The $className might choose one of the roles visible to play for a complete turn. Once the role has been played, it is discarded."
    }

    override fun clone() = Actor(this.order)
}