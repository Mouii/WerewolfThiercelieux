package Model;

import Technical.PowerEnum;

public class Witch extends GameCharacter {

    public Witch() {
        super("Witch",
                "The Witch has two potions; one of dead and one of resurrection, that she can use in the same time.",
                true,
                PowerEnum.Variable,
                "../../img/Thiercelieux.png");
    }
}
