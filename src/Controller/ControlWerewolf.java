package Controller;

import Model.*;
import Model.Character;

import javax.naming.ldap.Control;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlWerewolf {

    /***
     * List of characters with turn order
     */
    private Map<Character, Integer> listCharacters;

    /***
     * List of the characters in game
     */
    private List<Character> characterListInGame;

    /***
     * Array that contains the two choices of the thief
     */
    private Character[] thiefCharacters;

    /***
     * Attribut if night time or not
     */
    private boolean isNight = false;

    /***
     * Active turn order number
     */
    private int activeTurnNumber = 0;

    /***
     * Write the lovers in any way the master prefer
     */
    public String loveText = "";

    /***
     * Instance for singleton pattern
     */
    private static ControlWerewolf instance = null;

    /***
     * Getting instance for singleton
     * @return instance
     */
    public static ControlWerewolf GetInstance() {
        if(instance == null)
            instance = new ControlWerewolf();

        return instance;
    }

    private ControlWerewolf() {
        listCharacters = new HashMap<Character, Integer>();
        characterListInGame = new ArrayList<Character>();
        thiefCharacters = new Character[2];
        CreateListOfCharacters();
    }

    /***
     * Add character in game in preparations at the end
     * @param character
     */
    public void AddCharacter(Character character, int index) {
        characterListInGame.add(index, character);

        Map.Entry<Character, Integer> characterOrder = listCharacters.entrySet()
                .stream()
                .filter(x -> x.getKey().getClass().equals(character.getClass()))
                .findFirst().get();

        listCharacters.put(characterOrder.getKey(), characterOrder.getValue() - 1);
    }

    /***
     * Remove character in game
     * @param character
     */
    public void RemoveCharacterInGame(Character character) {
        characterListInGame.remove(character);

        Map.Entry<Character, Integer> characterOrder = listCharacters.entrySet()
                                                   .stream()
                                                   .filter(x -> x.getKey().getClass().equals(character.getClass()))
                                                   .findFirst().get();

        listCharacters.put(characterOrder.getKey(), characterOrder.getValue() + 1);
    }

    /***
     * Create all the data of characters with possible number in game
     */
    private void CreateListOfCharacters() {
        listCharacters.put(new Villager(), 6);
        listCharacters.put(new Werewolf(), 5);
        listCharacters.put(new Hunter(), 1);
        listCharacters.put(new Seer(), 1);
        listCharacters.put(new SmallGirl(), 1);
        listCharacters.put(new Witch(), 1);
        listCharacters.put(new Cupid(), 1);
        listCharacters.put(new Thief(), 1);
    }

    /***
     * Game loop
     */
    public void MainLoopGame() {

    }


    //Retirer un personnage en jeu

    //Déclarer mort un personnage

    //Avoir un tour actif

    //Détenir la liste d'ordre des personnages


}
