package Model;

import Technical.PowerEnum;

public class Angel extends GameCharacter {

    public Angel() {
        super("Angel",
                "The Angel is a character whose goal is to get killed on the first day or the first night.",
                false,
                PowerEnum.Variable,
                "../../resources/images/characters/Angel.jpg");
    }
}
