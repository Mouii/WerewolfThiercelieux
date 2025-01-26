package Model;

import Technical.PowerEnum;

public class Defender extends GameCharacter {

    public Defender() {
        super("Defender",
                "Each night, the defender protects one person. This person will be shielded and cannot die during the night.",
                true,
                PowerEnum.Constant,
                "../../resources/images/characters/Defender.jpg");
    }
}
