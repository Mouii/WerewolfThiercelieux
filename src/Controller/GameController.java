package Controller;

import Model.*;
import Technical.CharacterOrder;

import java.util.*;

public class GameController {

    /***
     * List of characters with turn order
     */
    private Map<String, Byte> listCharacters;

    /***
     * List of the characters in game
     */
    private List<CharacterOrder> characterListInGame;

    /***
     * Array that contains the two choices of the thief
     */
    private GameCharacter[] thiefCharacters;

    /***
     * Actif character display on night turn
     */
    private List<CharacterOrder> actifNightCharacter;

    /***
     * Last dead character to remember in case...
     */
    private CharacterOrder lastDeadCharacter;

    /***
     * If it is night time or not
     */
    private boolean isNight = false;

    /***
     * Game is active or not
     */
    private boolean activeGame = false;

    /***
     * Active turn order number
     */
    private byte activeTurnNumber = -1;

    /***
     * Write the lovers in any way the master prefer
     */
    private String loveText = "";

    /***
     * Instance for singleton pattern
     */
    private static GameController instance = null;

    /***
     * Getting instance for singleton
     * @return instance
     */
    public static GameController GetInstance() {
        if (instance == null)
            instance = new GameController();

        return instance;
    }

    private GameController() {

    }

    /***
     * Init all the data for a new game
     */
    public void InitGame() {
        listCharacters = new HashMap<String, Byte>();
        characterListInGame = new ArrayList<CharacterOrder>();
        actifNightCharacter = new ArrayList<CharacterOrder>();
        thiefCharacters = new GameCharacter[2];
        CreateListOfCharacters();
        isNight = false;
        activeGame = false;
        lastDeadCharacter = null;
        activeTurnNumber = -1;
    }

    /***
     * Getter list characters as the map
     * @return The Map of the character type and it's number allowed
     */
    public Map<String, Byte> GetListCharacters() {
        return listCharacters;
    }

    /***
     * Get the list of characters in game
     * @return The list of characters with their order
     */
    public List<CharacterOrder> GetCharacterListInGame() {
        return characterListInGame;
    }

    /***
     * Get the characters of the thief
     * @return The array of the thief's characters
     */
    public GameCharacter[] GetThiefCharacters() {
        return thiefCharacters;
    }

    /***
     * Getting the list of actives characters
     * @return The list of actives characters
     */
    public List<CharacterOrder> GetActiveNightCharacter() {
        return actifNightCharacter;
    }

    /***
     * Get the last dead character
     * @return The last dead character with it's order
     */
    public CharacterOrder GetLastDeadCharacter() {
        return lastDeadCharacter;
    }

    /***
     * Get the active turn number
     * @return The active turn number
     */
    public byte GetActiveTurnNumber() {
        return activeTurnNumber;
    }

    /***
     * Night mode or not
     * @return Statement of the night
     */
    public boolean IsNight() {
        return isNight;
    }

    /***
     * Set the night mode
     * @param night Night mode to define
     */
    public void SetNight(boolean night) {
        isNight = night;
    }

    /***
     * Set the game active or not
     * @param activeGame Activate the game or not
     */
    public void SetActiveGame(boolean activeGame) {
        this.activeGame = activeGame;
    }

    /***
     * Reset the active turn number
     */
    public void ResetActiveTurnNumber() {
        activeTurnNumber = -1;
    }

    /***
     * Set the love text from Cupid
     * @param loveText Love text to put
     */
    public void SetLoveText(String loveText) {
        this.loveText = loveText;
    }

    /***
     * Add character in game in preparations at the end
     * @param characterOrder Character to add
     */
    public void AddCharacter(CharacterOrder characterOrder) {

        //First update order
        characterListInGame.stream()
                .filter(x -> x.GetOrder() > characterOrder.GetOrder()).forEach(
                        x -> x.UpOrder());

        //Then add the good one
        characterListInGame.add(characterOrder);
    }

    /***
     * Remove character in game
     * @param characterOrder Character to remove
     */
    public void RemoveCharacterInGame(CharacterOrder characterOrder) {
        //First update order
        characterListInGame.stream()
                .filter(x -> x.GetOrder() > characterOrder.GetOrder()).forEach(
                        x -> x.DownOrder());

        //Then add the good one
        characterListInGame.remove(characterOrder);

        //Update the last dead character if active game
        if (activeGame) {
            // Memory of the last dead
            lastDeadCharacter = characterOrder;
        }

    }

    /***
     * Function that execute the update for the next character turn
     * Pass to the next nocturnal character or start the beginning of the day
     */
    public void NextNightTurn() {

        int maxSize = characterListInGame.size();

        do {
            activeTurnNumber++;

            if (activeTurnNumber == maxSize) {
                actifNightCharacter = null;
            } else {
                actifNightCharacter = GetCharactersFromTurn(activeTurnNumber);
            }

        } while (activeTurnNumber < maxSize && actifNightCharacter.isEmpty());
    }

    /***
     * Allow the reintroduction of the last dead character
     */
    public void PutBackLastDeadCharacter() {
        int index = lastDeadCharacter.GetOrder();

        if (index < characterListInGame.size())
            AddCharacter(lastDeadCharacter);
        else {
            lastDeadCharacter.SetOrder((byte) (characterListInGame.size() - 1));
            AddCharacter(lastDeadCharacter);
            lastDeadCharacter = null;
        }
    }

    /***
     * Basic thief can have two other characters
     * @param gameCharacter Character to add for thief
     */
    public void AddCharacterForThief(GameCharacter gameCharacter) {
        if (thiefCharacters[0] == null)
            thiefCharacters[0] = gameCharacter;
        else if (thiefCharacters[1] == null)
            thiefCharacters[1] = gameCharacter;
    }

    /***
     * Remove a character from the thief's list
     * @param gameCharacter Character to remove
     */
    public void RemoveCharacterForThief(GameCharacter gameCharacter) {
        if (thiefCharacters[0].equals(gameCharacter))
            thiefCharacters[0] = null;
        else if (thiefCharacters[1].equals(gameCharacter))
            thiefCharacters[1] = null;
    }

    /***
     * Create all the data of characters with possible number in game
     */
    private void CreateListOfCharacters() {

        //Max villagers possible at the same time
        byte maxVillagers = 6;
        listCharacters.put("Villager", maxVillagers);

        //Max normal werewolves possible at the same time
        byte maxWerewolves = 5;
        listCharacters.put("Werewolf", maxWerewolves);

        //Unique class
        byte unique = 1;
        listCharacters.put("Seer", unique);
        listCharacters.put("Witch", unique);
        listCharacters.put("Cupid", unique);
        listCharacters.put("LittleGirl", unique);
        listCharacters.put("Thief", unique);

    }

    /***
     * Return the next characters for the corresponding turn
     * @param order Order of the night
     * @return A list of active characters in the order defined. Empty if none.
     */
    private List<CharacterOrder> GetCharactersFromTurn(byte order) {

        return characterListInGame
                .stream()
                .filter(x -> x.GetOrder() == order && x.GetCharacter().isNocturnal())
                .toList();

    }

    /***
     * Change the nocturnal nature of the character
     * @param characterOrder The character with its order
     */
    public void ChangeNocturnalStatement(CharacterOrder characterOrder) {
        characterOrder.GetCharacter().changeNocturnal();
    }

    public boolean IsCharacterWerewolf(GameCharacter gameCharacter) {
        return gameCharacter instanceof Werewolf
                || gameCharacter instanceof BigBadWolf
                || gameCharacter instanceof WhiteWerewolf
                // gameCharacter instanceof WolfFather
                || (gameCharacter instanceof WildChild && gameCharacter.isNocturnal())
                //|| (gameCharacter instanceof WolfHound && gameCharacter.isNocturnal())
                || gameCharacter.isInfected();
    }

}