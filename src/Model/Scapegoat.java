package Model;

import Technical.PowerEnum;

public class Scapegoat extends GameCharacter {

    public Scapegoat() {
        super("Scapegoat",
                "If the village vote results in a tie, the Scapegoat is eliminated instead. He then decide who will not be allowed to vote during the next day.",
                false,
                PowerEnum.Constant,
                "../../resources/images/characters/Scapegoat.jpg");
    }
}
