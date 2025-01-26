package Model;

import Technical.PowerEnum;

public class Elder extends GameCharacter {

    public Elder() {
        super("Elder",
                "The elder has two lives during the night. When he is supposed to be killed by the werewolves, he lose one life without being notified.",
                false,
                PowerEnum.Constant,
                "../../resources/images/characters/Elder.jpg");
    }
}