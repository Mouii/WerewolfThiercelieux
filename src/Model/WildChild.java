package Model;

public class WildChild extends Character {

    public WildChild() {
        super("WildChild",
                "At the start of the game, the Wild Child chooses a mentor. This player is unaware that they have been chosen as the Wild Child’s mentor. If, during the game, the mentor dies, the Wild Child becomes a werewolf.",
                true,
                PowerEnum.Constant,
                "../../resources/images/characters/WildChild.jpg");
    }
}
