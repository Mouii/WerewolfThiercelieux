package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import model.ConditionalActivation
import model.PowerState
import model.RoleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * The powers that wake up because of what happens in the game
 */
class PowerRulesTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 3)
    }

    @Test
    fun `a power waiting for a werewolf to die wakes up when one dies`() {
        val wolf = give(0, role(RoleId.WEREWOLF, isWerewolf = true))
        val waiting = give(
            1,
            role("Fox", powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.ONE_WEREWOLF, isNocturnal = true)
        )
        waiting.isNocturnal = false

        RoleRoster.kill(GameState.seats[0])

        assertEquals(PowerState.PERMANENT, waiting.powerState)
        assertTrue("it is called again from now on", waiting.isNocturnal)
        assertFalse(GameState.aliveCharacters.any { it === wolf })
    }

    @Test
    fun `a power waiting for a villager does not wake up when a werewolf dies`() {
        give(0, role(RoleId.WEREWOLF, isWerewolf = true))
        val waiting = give(
            1,
            role("Crow", powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.ONE_VILLAGER)
        )

        RoleRoster.kill(GameState.seats[0])

        assertEquals(PowerState.CONDITIONAL, waiting.powerState)
    }

    @Test
    fun `a power waiting for its linked role wakes up when the last one dies`() {
        give(0, role("Sister", name = "Sister"))
        val waiting = give(
            1,
            role(
                "Brother",
                powerState = PowerState.CONDITIONAL,
                condition = ConditionalActivation.LINKED_PLAYERS,
                rolesToStick = arrayOf("SISTER-NORMAL")
            )
        )

        RoleRoster.kill(GameState.seats[0])

        assertEquals(PowerState.PERMANENT, waiting.powerState)
    }

    @Test
    fun `the elder power takes the powers of the villagers away`() {
        val villager = give(0, role("Seer", isNocturnal = true))
        val wolf = give(1, role(RoleId.WEREWOLF, isWerewolf = true, isNocturnal = true))

        PowerRules.removeVillagersPowers()

        assertFalse("the villagers lose their night", villager.isNocturnal)
        assertEquals(PowerState.PERMANENT, villager.powerState)
        assertTrue("the werewolves keep theirs", wolf.isNocturnal)
        assertTrue(GameState.eldersPowerRemoved)
        assertEquals(GameMessage.ELDER_POWER_USED, GameState.history.last().text.message)
    }

    @Test
    fun `the wild child plays with the pack without being called`() {
        give(0, role(RoleId.WEREWOLF, isWerewolf = true), atOrder = 40)
        val child = give(1, role(RoleId.WILD_CHILD), atOrder = 10)
        RoleRoster.updateWerewolfTurn()

        PowerRules.setupWildChild(child)

        assertEquals(GameState.werewolfTurn, child.order)
        assertTrue(child.isWerewolf)
        assertFalse("he is not woken up by himself", child.isNocturnal)
    }
}
