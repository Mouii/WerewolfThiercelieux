package Model;

public class BearTamer extends Character {

    public BearTamer() {
        super("BearTamer",
                "Each morning, if the Bear Tamer is sitting to the right or left of a werewolf, the bear growls (indicated by the Game Master). If the Bear Tamer is infected by the Werewolf Elder, the bear will growl until the end of the game.",
                false,
                PowerEnum.Constant,
                "../../resources/images/characters/BearTamer.jpg");
    }
}
