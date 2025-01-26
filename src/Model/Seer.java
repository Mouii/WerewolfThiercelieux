package Model;

import Technical.PowerEnum;

public class Seer extends GameCharacter {
    public Seer() {
        super("Seer",
                "The Seer can see the card of another player one time per turn.",
                true,
                PowerEnum.Constant,
                "../../img/Thiercelieux.png");
    }
}
