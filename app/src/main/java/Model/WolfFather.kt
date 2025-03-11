package Model

class WolfFather(order: Int)
    : Character("The Wolf Father has the power to convert a victim into a werewolf. One time per game, the Wolf Father can give a sign to the animator to infect the victim. The victim will not be eliminated and will wake up with the others on the werewolves turn."
    , true
    , PowerState.VARIABLE
    , true
    , order
    , 1) {

    override fun action(): String {
        return "The $className can infect a victim in order to add one more werewolf to the pack instead of killing it."
    }

    override fun clone() = WolfFather(this.order)
}