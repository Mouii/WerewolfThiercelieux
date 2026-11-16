package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * The night : which roles are called, in which order, and the moves of the moderator in it
 */
class NightFlowTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 3)
    }

    @Test
    fun `only the moments where somebody is awake are called`() {
        give(0, role("Seer", isNocturnal = true), atOrder = 10)
        give(1, role("Villager", isNocturnal = false), atOrder = 20)

        NightFlow.freeze()

        assertEquals(listOf(10), GameState.nightSteps)
    }

    @Test
    fun `the moderator walks the night forwards and backwards`() {
        give(0, role("Seer"), atOrder = 10)
        give(1, role("Witch"), atOrder = 30)
        NightFlow.freeze()

        assertEquals(10, NightFlow.next())
        assertEquals(30, NightFlow.next())
        assertNull("the night is over after the last role", NightFlow.next())

        GameState.phase = EnumPhase.NIGHT
        assertEquals(10, NightFlow.previous())
        assertNull("there is nothing before the first role", NightFlow.previous())
    }

    @Test
    fun `a role that is not called tonight cannot be jumped to`() {
        give(0, role("Seer"), atOrder = 10)
        NightFlow.freeze()

        assertTrue(NightFlow.jumpTo(10))
        assertFalse("nobody is awake at this moment", NightFlow.jumpTo(999))
    }

    @Test
    fun `calling a role says its name and its action`() {
        give(0, role("Seer", name = "Seer"), atOrder = 10)
        NightFlow.freeze()
        NightFlow.next()

        val call = NightFlow.enter(10)

        assertEquals(1, call.roles.size)
        assertEquals("Seer", call.roles[0].roleName)
        assertEquals("action of Seer", call.roles[0].action)
    }

    @Test
    fun `a role is only called once a night, so a power used once is not lost`() {
        val thief = give(0, role("Thief", powerState = model.PowerState.SETUP), atOrder = 10)
        NightFlow.freeze()
        NightFlow.next()

        val first = NightFlow.enter(10)
        thief.isNocturnal = true          //the moderator wakes him up again by hand
        val second = NightFlow.enter(10)

        assertEquals(first.roles[0].action, second.roles[0].action)
        assertTrue("the second call must not play the power again", thief.isNocturnal)
    }

    @Test
    fun `the pack of werewolves is called with the infected players`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 40)
        val villager = give(1, role("Seer"), atOrder = 10)
        RoleRoster.infect(GameState.seats[1])

        RoleRoster.updateWerewolfTurn()
        NightFlow.freeze()
        val call = NightFlow.enter(GameState.werewolfTurn)

        assertEquals("the infected player follows the pack", listOf("Player 2"), call.infectedPlayers)
        assertTrue(villager.isWerewolf)
    }

    @Test
    fun `swapping two moments keeps the role that is being called`() {
        val seer = give(0, role("Seer"), atOrder = 10)
        val witch = give(1, role("Witch"), atOrder = 30)
        NightFlow.freeze()
        GameState.phase = EnumPhase.NIGHT
        NightFlow.next()                     //the seer is called

        NightFlow.swapOrders(10, 30)

        assertEquals(30, seer.order)
        assertEquals(10, witch.order)
        assertEquals("the seer stays the called role", 30, GameState.nightSteps[GameState.stepPos])
    }

    @Test
    fun `waking a role up during the night only takes effect when the night ends`() {
        val villager = give(0, role("Villager", isNocturnal = false), atOrder = 20)
        NightFlow.freeze()
        GameState.phase = EnumPhase.NIGHT

        NightFlow.setAwakeGroup(listOf(villager), awake = true, label = "Villager")

        assertFalse("still asleep tonight", villager.isNocturnal)
        assertTrue("but shown as waking up", GameState.shownAwake(villager))

        NightFlow.applyPending()
        assertTrue(villager.isNocturnal)
    }

    @Test
    fun `waking a role up outside of the night takes effect at once`() {
        val villager = give(0, role("Villager", isNocturnal = false), atOrder = 20)
        GameState.phase = EnumPhase.DAY

        NightFlow.setAwakeGroup(listOf(villager), awake = true, label = "Villager")

        assertTrue(villager.isNocturnal)
        assertEquals(GameMessage.ROLES_AWAKE, GameState.history.last().text.message)
    }
}
