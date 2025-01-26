package Model;

import Technical.PowerEnum;

public class BigBadWolf extends GameCharacter {

    public BigBadWolf() {
        super("BigBadWolf",
                "Each night, the defender protects one person. This person will be shielded and cannot die during the night.",
                true,
                PowerEnum.Constant,
                "../../resources/images/characters/Defender.jpg");
    }
}
