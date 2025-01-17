package Model;

public class WhiteWerewolf extends Character {

    public WhiteWerewolf() {
        super("WhiteWerewolf",
                "The WhiteWerewolf wins alone by eliminating both the entire village and the werewolves. Every 2 nights, he can devour a werewolf immediately after the werewolves turn. He wake up and vote at the same time as the werewolves.",
                true,
                PowerEnum.Constant,
                "../../resources/images/characters/WhiteWerewolf.jpg");
    }
}
