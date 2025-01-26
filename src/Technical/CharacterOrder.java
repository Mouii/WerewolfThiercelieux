package Technical;

import Model.GameCharacter;

/***
 * Class that handle order for characters
 */
public class CharacterOrder {

    /***
     * Character in question
     */
    private final GameCharacter character;

    /***
     * Order of the character
     */
    private byte order;

    /***
     * Constructor
     * @param character Concerned character
     * @param order Order of the character
     */
    public CharacterOrder(GameCharacter character, byte order) {
        this.character = character;
        this.order = order;
    }

    /***
     * Getter character
     * @return Character
     */
    public GameCharacter GetCharacter() {
        return character;
    }

    /***
     * Getter order
     * @return Order
     */
    public byte GetOrder() {
        return order;
    }

    /***
     * Setter of the order
     * @param order New order
     */
    public void SetOrder(byte order) {
        this.order = order;
    }

    /***
     * Add 1 to the order
     */
    public void UpOrder() {
        order++;
    }

    /***
     * Remove 1 to the order
     */
    public void DownOrder() {
        order--;
    }
}
