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
 * The roles of the game : given to a player, taken away, and the deaths
 */
class RoleRosterTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 2)
    }

    @Test
    fun `a role given to a player is a copy, placed at its usual moment`() {
        val prototype = role("Seer")

        val given = give(0, prototype)

        assertEquals("Seer", given.className)
        assertFalse("the player holds his own copy", given === prototype)
        assertEquals(RoleOrders.orderOf("Seer"), given.order)
        assertEquals(1, RoleRoster.countOfClass("Seer"))
    }

    @Test
    fun `two players of the same role play at the same moment`() {
        val first = give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true))
        val second = give(1, role("Werewolf", isWerewolf = true, definesPackTurn = true))

        assertEquals(first.order, second.order)
    }

    @Test
    fun `a role that chooses players gets its own block`() {
        give(0, role("Seer", doTargeting = true, name = "Seer"))

        val block = GameState.effectBlocks.firstOrNull { it.kind == EffectBlock.Kind.TARGETS }
        assertTrue("the block of the seer must exist", block != null)
        assertEquals("Seer", block?.title)
    }

    @Test
    fun `taking the role away removes its block`() {
        give(0, role("Seer", doTargeting = true))

        RoleRoster.setRole(GameState.seats[0], null)

        assertNull(GameState.seats[0].character)
        assertTrue(GameState.effectBlocks.none { it.kind == EffectBlock.Kind.TARGETS })
    }

    @Test
    fun `a player who dies leaves the turn order`() {
        val seer = give(0, role("Seer"))
        give(1, role("Werewolf", isWerewolf = true, definesPackTurn = true))

        val gameOver = RoleRoster.kill(GameState.seats[0])

        assertFalse("one player still holds a role", gameOver)
        assertFalse(GameState.seats[0].alive)
        assertTrue(GameState.aliveCharacters.none { it === seer })
        assertTrue("a dead role is still known by the game", GameState.allCharacters.any { it === seer })
        assertEquals(GameMessage.PLAYER_DEAD, GameState.history.last().text.message)
    }

    @Test
    fun `the game is over when nobody holds a role anymore`() {
        newGame(playerCount = 1)
        give(0, role("Seer"))

        val gameOver = RoleRoster.kill(GameState.seats[0])

        assertTrue(gameOver)
        assertFalse(GameState.inProgress)
    }

    @Test
    fun `a player brought back to the game reopens a game that was over`() {
        newGame(playerCount = 1)
        give(0, role("Seer"))
        RoleRoster.kill(GameState.seats[0])

        val reopened = RoleRoster.resurrect(GameState.seats[0])

        assertTrue(reopened)
        assertTrue(GameState.inProgress)
        assertTrue(GameState.seats[0].alive)
        assertEquals(1, GameState.aliveCharacters.size)
    }

    @Test
    fun `the pack of werewolves follows the simple werewolf`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true))

        RoleRoster.updateWerewolfTurn()

        assertEquals(GameState.seats[0].character?.order, GameState.werewolfTurn)
    }

    @Test
    fun `without werewolf, and the pack turned off, nobody calls it`() {
        give(0, role("Seer"))

        RoleRoster.updateWerewolfTurn()

        assertEquals(-1, GameState.werewolfTurn)
    }

    @Test
    fun `the players marked to die are eliminated when the turn changes`() {
        give(0, role("Seer"))
        give(1, role("Werewolf", isWerewolf = true, definesPackTurn = true))
        GameState.deathsBlock()?.players?.add(GameState.seats[0])

        RoleRoster.applyPendingDeaths()

        assertFalse(GameState.seats[0].alive)
        assertTrue("the block is empty again", GameState.deathsBlock()?.players?.isEmpty() == true)
    }

    @Test
    fun `an infected player joins the werewolves but keeps his role`() {
        val seer = give(0, role("Seer"))

        RoleRoster.infect(GameState.seats[0])

        assertTrue(seer.isWerewolf)
        assertTrue(seer.isInfected)
        assertEquals("Seer", seer.className)

        RoleRoster.disinfect(GameState.seats[0])
        assertFalse(seer.isWerewolf)
        assertFalse(seer.isInfected)
    }
}
