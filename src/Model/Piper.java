package Model;

import Technical.PowerEnum;

public class Piper extends GameCharacter {

    public Piper() {
        super("Piper",
                "The Piper wakes up last. He can then charm two players, who will become the Charmed. The Piper wins when all living players are charmed.",
                true,
                PowerEnum.Constant,
                "../../resources/images/characters/Piper.jpg");
    }
}
