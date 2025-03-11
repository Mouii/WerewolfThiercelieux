package Model

class Sister(order: Int)
    : Character("The sisters wake up at night and know each others. This can be a big impact about trust. They can communicate at night with signs."
    , true
    , PowerState.PERMANENT
    , false
    , order
    , 2) {

    override fun action(): String {
        return "The $className recognize each others and can communicate through signs together."
    }

    override fun clone() = Sister(this.order)
}