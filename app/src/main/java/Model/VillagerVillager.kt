package Model

class VillagerVillager(order: Int)
    : Character("The villager is someone who everybody knows. He/she can't hide his/her role."
    , false
    , PowerState.PERMANENT
    , false
    , order
    , 1) {

    override fun clone() = VillagerVillager(this.order)
}