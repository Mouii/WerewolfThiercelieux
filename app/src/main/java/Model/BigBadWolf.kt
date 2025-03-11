package Model

class BigBadWolf(order: Int)
    : Character("The big bad wolf can eat a second victim during the night. He/she eats with the others on the common turn then has a unique turn for him/her. This power is lost when a werewolf, wild child, or wolfhound is dead."
    , true
    , PowerState.VARIABLE
    , true
    , order
    , 1) {

    override fun action(): String {
        return "The $className choose a victim to eat. On its only turn, he can eat another victim"
    }

    override fun clone() = BigBadWolf(this.order)
}