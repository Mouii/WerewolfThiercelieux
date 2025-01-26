package Model;

import Technical.PowerEnum;

public class Hunter extends GameCharacter {

    public Hunter() {
        super("Hunter",
                "The Hunter can, when he dies, kill someone else alive.",
                false,
                PowerEnum.Uniq,
                "../../img/Thiercelieux.png");
    }
}
