package model

import GameFixtures.role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import model.NightSlot
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/***
 * The role itself : its copies, and what happens when it is called during the night
 */
class CharacterGameTest {

    @Test
    fun `a copy of a role never changes the role it comes from`() {
        val source = role("Seer", isNocturnal = true)

        val copy = source.clone()
        copy.isNocturnal = false
        copy.name = "Another name"

        assertNotSame(source, copy)
        assertTrue("the role of the roles file must not change", source.isNocturnal)
        assertEquals("Seer", source.name)
    }

    @Test
    fun `a copy keeps the name of the role, which is its identity`() {
        val copy = role("WolfFather").clone()

        assertEquals("WolfFather", copy.className)
    }

    @Test
    fun `a role given to a player is placed at its moment and is not infected`() {
        val source = role("Witch", order = 10)
        source.isInfected = true

        val given = source.copyAt(70)

        assertEquals(70, given.order)
        assertFalse("a role given to a player starts clean", given.isInfected)
    }

    @Test
    fun `a copy keeps how the role takes part in the night`() {
        val source = role(
            "LittleGirl",
            playsAt = NightSlot.WEREWOLF_TURN,
            hasSeparateTurn = true,
            ownsInfectionBlock = true,
            definesPackTurn = true
        )

        val copy = source.clone()

        assertEquals(NightSlot.WEREWOLF_TURN, copy.playsAt)
        assertTrue(copy.hasSeparateTurn)
        assertTrue(copy.ownsInfectionBlock)
        assertTrue(copy.definesPackTurn)
    }

    @Test
    fun `a role given to a player has not changed yet`() {
        val source = role("WildChild")
        source.changeApplied = true

        assertFalse(source.copyAt(10).changeApplied)
    }

    @Test
    fun `a power used only once stops waking up its player`() {
        val thief = role("Thief", powerState = PowerState.SETUP, isNocturnal = true)

        val said = thief.nightAction()

        assertEquals("action of Thief", said)
        assertFalse("the role must not be called again", thief.isNocturnal)
    }

    @Test
    fun `a conditional power of a villager is over once it played`() {
        val judge = role(
            "StutteringJudge",
            powerState = PowerState.CONDITIONAL,
            condition = ConditionalActivation.ONE_VILLAGER,
            isWerewolf = false
        )

        judge.nightAction()

        assertFalse(judge.isNocturnal)
    }

    @Test
    fun `a role that has a turn of its own stays awake after its action`() {
        val wolf = role(
            "BigBadWolf",
            powerState = PowerState.SETUP,
            isWerewolf = true,
            isNocturnal = true,
            hasSeparateTurn = true
        )

        wolf.nightAction()

        assertTrue("it eats with the pack and then once alone", wolf.isNocturnal)
    }

    @Test
    fun `a permanent power keeps waking up its player`() {
        val seer = role("Seer", powerState = PowerState.PERMANENT)

        seer.nightAction()

        assertTrue(seer.isNocturnal)
    }
}
