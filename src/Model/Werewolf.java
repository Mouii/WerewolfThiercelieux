package Model;

import Technical.PowerEnum;

public class Werewolf extends GameCharacter {

    public Werewolf() {
        super("Werewolf",
                "The werewolf can select a victim during the night to eat and eliminate from the game.",
                true,
                PowerEnum.Constant,
                "../../img/Thiercelieux.png");
    }
}
