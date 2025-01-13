package Model;

public class Werewolf extends Character {

    public Werewolf() {
        super("Werewolf",
                "The werewolf can select a victim during the night to eat and eliminate from the game.",
                true,
                PowerEnum.Constant,
                "../../img/Thiercelieux.png");
    }
}
