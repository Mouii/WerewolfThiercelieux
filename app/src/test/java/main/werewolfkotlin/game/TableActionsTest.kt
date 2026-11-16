package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * What the moderator does by tapping the players : a victim, a target, the roles at random
 */
class TableActionsTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 3)
    }

    @Test
    fun `the victim of the werewolves goes in the block of the deaths`() {
        val wolf = give(0, role("Werewolf", isWerewolf = true, doKilling = true))
        GameState.activeNow = listOf(wolf)

        val said = TableActions.toggleKill(GameState.seats[1])

        assertEquals(GameMessage.DEATH_TARGET, said?.message)
        assertEquals(listOf(wolf.name, "Player 2"), said?.names)
        assertTrue(GameState.deathsBlock()?.players?.any { it === GameState.seats[1] } == true)
    }

    @Test
    fun `choosing the same victim again takes it back`() {
        val wolf = give(0, role("Werewolf", isWerewolf = true, doKilling = true))
        GameState.activeNow = listOf(wolf)

        TableActions.toggleKill(GameState.seats[1])
        val said = TableActions.toggleKill(GameState.seats[1])

        assertEquals(GameMessage.DEATH_TARGET_REMOVED, said?.message)
        assertTrue(GameState.deathsBlock()?.players?.isEmpty() == true)
    }

    @Test
    fun `no called role kills, so tapping a player kills nobody`() {
        val seer = give(0, role("Seer"))
        GameState.activeNow = listOf(seer)

        assertNull(TableActions.toggleKill(GameState.seats[1]))
    }

    @Test
    fun `the target of a role goes in the block of this role`() {
        val seer = give(0, role("Seer", doTargeting = true, name = "Seer"))
        GameState.activeNow = listOf(seer)

        val said = TableActions.toggleTarget(GameState.seats[1])

        assertEquals(GameMessage.TARGET, said?.message)
        val block = GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS }
        assertEquals(listOf(GameState.seats[1]), block.players.toList())
    }

    @Test
    fun `the roles are given at random to the players who have none`() {
        val chosen = listOf(role("Seer"), role("Witch"))

        TableActions.giveRolesAtRandom(chosen, replaced = emptyList())

        val given = GameState.seats.mapNotNull { it.character?.className }
        assertEquals(2, given.size)
        assertTrue(given.containsAll(listOf("Seer", "Witch")))
        assertEquals("the third player waits for a role", 1, TableActions.roleless().size)
    }

    @Test
    fun `a replaced player gets a new role`() {
        give(0, role("Villager"))

        TableActions.giveRolesAtRandom(listOf(role("Seer")), replaced = listOf(GameState.seats[0]))

        val given = GameState.seats.mapNotNull { it.character?.className }
        assertEquals(listOf("Seer"), given)
    }
}
