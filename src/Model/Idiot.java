package Model;

import Technical.PowerEnum;

public class Idiot extends GameCharacter {

    public Idiot() {
        super("Idiot",
                "If the Idiot is chosen by the village vote, they do not die.",
                false,
                PowerEnum.Constant,
                "../../resources/images/characters/VillageIdiot.jpg");
    }
}
