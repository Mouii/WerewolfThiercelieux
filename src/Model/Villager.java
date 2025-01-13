package Model;

public class Villager extends Character {

    public Villager() {
        super("Villager",
                "The villager has no other power than its own deduction to find out the werewolves.",
                false,
                PowerEnum.Constant,
                "../../img/Thiercelieux.png");
    }
}
