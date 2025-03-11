package Model

class Brother(order: Int)
    : Character("The brothers wake up at night and know each others. This can be a big impact about trust. They can communicate at night with signs."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 3) {

    override fun action(): String {
        return "The $className recognize each others and can communicate through signs together."
    }

    override fun clone() = Brother(this.order)
}