package Model;

public class Thief extends Character {
    public Thief() {
        super("Thief",
                "The Thief can steal the card of another player during the night.",
                true,
                PowerEnum.Uniq,
                "../../resources/images/characters/Thief.jpg");
    }
}


