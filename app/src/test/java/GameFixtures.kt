import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.RoleOrders
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import model.CharacterGame
import model.ConditionalActivation
import model.NightSlot
import model.PowerState
import model.RoleChange

/***
 * Short ways to build a game in a test : a role, a table of players, a role given to a player.
 *
 * Every test starts from an empty game (see newGame) because the game is held in one shared
 * object (GameState) : without this, a test would see what the test before it did.
 */
object GameFixtures {

    /***
     * A role, with the values a test does not care about already filled
     */
    fun role(
        className: String,
        name: String = className,
        powerState: PowerState = PowerState.PERMANENT,
        isNocturnal: Boolean = true,
        isWerewolf: Boolean = false,
        isSolo: Boolean = false,
        condition: ConditionalActivation = ConditionalActivation.NO_CONDITION,
        doTargeting: Boolean = false,
        doKilling: Boolean = false,
        source: String = "NORMAL",
        playsAt: NightSlot = NightSlot.OWN_MOMENT,
        hasSeparateTurn: Boolean = false,
        ownsInfectionBlock: Boolean = false,
        definesPackTurn: Boolean = false,
        becomes: RoleChange? = null,
        order: Int = 0
    ): CharacterGame {
        val character = CharacterGame(
            className = className,
            name = name,
            description = "description of $name",
            action = "action of $name",
            powerState = powerState,
            isNocturnal = isNocturnal,
            isWerewolf = isWerewolf,
            isSolo = isSolo,
            maxOccurrence = 4,
            condition = condition,
            doTargeting = doTargeting,
            doKilling = doKilling,
            source = source,
            playsAt = playsAt,
            hasSeparateTurn = hasSeparateTurn,
            ownsInfectionBlock = ownsInfectionBlock,
            definesPackTurn = definesPackTurn,
            becomes = becomes
        )
        character.order = order
        return character
    }

    /***
     * An empty game with [playerCount] players around the table and nobody holding a role
     */
    fun newGame(playerCount: Int = 3) {
        GameState.reset()
        //The pack without wolves is tested on its own : here a game only holds the roles it is given
        GameState.wolfTurnForced = false
        RoleOrders.replaceAll(emptyMap())
        repeat(playerCount) { index -> GameState.seats.add(Seat("Player " + (index + 1))) }
    }

    /***
     * Give a role to the player of this seat, and return the copy the player holds.
     *
     * The moment of the night does not come from the role but from the turn order of the game,
     * so [atOrder] puts the role at a known moment first, which keeps the tests readable.
     */
    fun give(seatIndex: Int, prototype: CharacterGame, atOrder: Int? = null): CharacterGame {
        if (atOrder != null) RoleOrders.set(prototype.className, atOrder)
        RoleRoster.setRole(GameState.seats[seatIndex], prototype)
        return GameState.seats[seatIndex].character!!
    }
}
