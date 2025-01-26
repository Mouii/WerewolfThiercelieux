package Model;

import Technical.PowerEnum;

public class Thief extends GameCharacter {
    public Thief() {
        super("Thief",
                "The Thief can steal the card of another player during the night.",
                true,
                PowerEnum.Uniq,
                "../../resources/images/characters/Thief.jpg");
    }
}


