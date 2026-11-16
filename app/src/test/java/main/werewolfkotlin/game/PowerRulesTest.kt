package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import model.ConditionalActivation
import model.NightSlot
import model.PowerState
import model.RoleChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * The roles that change because of what happens in the game : their condition, and what they become
 */
class PowerRulesTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 3)
    }

    // ── What the roles file describes ──────────────────────────────

    @Test
    fun `alone, the sisters stop waking up`() {
        val first = give(0, sister())
        give(1, sister())

        RoleRoster.kill(GameState.seats[1])

        assertFalse("nobody left to talk to", first.isNocturnal)
        assertEquals(PowerState.PERMANENT, first.powerState)
        assertTrue(first.changeApplied)
    }

    @Test
    fun `two sisters alive keep waking up`() {
        val first = give(0, sister())
        give(1, sister())
        give(2, role("Villager", isNocturnal = false))

        RoleRoster.kill(GameState.seats[2])

        assertTrue(first.isNocturnal)
        assertFalse(first.changeApplied)
    }

    @Test
    fun `a variant of the same role can become a solo killer instead`() {
        //Same condition as the sisters, another change : the file decides, not the code
        val variant = give(0, sister().let {
            role(
                "Sister", name = "Last Sister", powerState = PowerState.CONDITIONAL,
                condition = ConditionalActivation.IS_ALONE, source = "CUSTOM",
                becomes = RoleChange(isSolo = true, doKilling = true)
            )
        })
        give(1, role("Villager", isNocturnal = false))

        RoleRoster.kill(GameState.seats[1])

        assertTrue(variant.isSolo)
        assertTrue(variant.doKilling)
    }

    @Test
    fun `the wild child joins the pack when its mentor dies`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 40)
        val child = give(1, wildChild(), atOrder = 10)

        //The moderator records the mentor : the player of the third seat
        val block = GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS }
        block.players.add(GameState.seats[2])

        RoleRoster.kill(GameState.seats[2])

        assertTrue("it becomes a werewolf", child.isWerewolf)
        assertTrue(child.isNocturnal)
        assertEquals(NightSlot.WEREWOLF_TURN, child.playsAt)
        assertEquals("it plays with the pack", GameState.werewolfTurn, child.order)
    }

    @Test
    fun `the wild child does not change when another player dies`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 40)
        val child = give(1, wildChild(), atOrder = 10)
        GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS }
            .players.add(GameState.seats[2])

        //The player who dies is not the mentor
        RoleRoster.kill(GameState.seats[0])

        assertFalse(child.isWerewolf)
    }

    @Test
    fun `a change that fires once does not happen again`() {
        newGame(playerCount = 4)
        val child = give(0, wildChild(), atOrder = 10)
        val block = GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS }
        block.players.add(GameState.seats[1])
        block.players.add(GameState.seats[2])

        RoleRoster.kill(GameState.seats[1])
        child.playsAt = NightSlot.OWN_MOMENT        //as if the moderator moved it back

        RoleRoster.kill(GameState.seats[2])

        assertEquals("the change must not be applied twice", NightSlot.OWN_MOMENT, child.playsAt)
    }

    @Test
    fun `the big bad wolf loses its own turn when a wolf dies`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 40)
        val bigBad = give(1, bigBadWolf(), atOrder = 180)
        give(2, role("Villager", isNocturnal = false))

        RoleRoster.kill(GameState.seats[0])

        assertEquals(NightSlot.WEREWOLF_TURN, bigBad.playsAt)
        assertEquals(PowerState.PERMANENT, bigBad.powerState)
    }

    @Test
    fun `the big bad wolf keeps its own turn while no wolf died`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 40)
        val bigBad = give(1, bigBadWolf(), atOrder = 180)
        give(2, role("Seer"))

        RoleRoster.kill(GameState.seats[2])

        assertEquals(NightSlot.OWN_MOMENT, bigBad.playsAt)
        assertEquals(180, bigBad.order)
    }

    @Test
    fun `the little girl is not a wolf, so her death changes nothing for the big bad wolf`() {
        give(0, role("LittleGirl", playsAt = NightSlot.WEREWOLF_TURN))
        val bigBad = give(1, bigBadWolf(), atOrder = 180)

        RoleRoster.kill(GameState.seats[0])

        assertEquals(NightSlot.OWN_MOMENT, bigBad.playsAt)
    }

    // ── Conditions the file does not describe ──────────────────────

    @Test
    fun `a role without a change only wakes up again`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true))
        val waiting = give(
            1,
            role(
                "Fox",
                powerState = PowerState.CONDITIONAL,
                condition = ConditionalActivation.ONE_WEREWOLF,
                isNocturnal = true
            )
        )
        waiting.isNocturnal = false

        RoleRoster.kill(GameState.seats[0])

        assertEquals(PowerState.PERMANENT, waiting.powerState)
        assertTrue("it is called again from now on", waiting.isNocturnal)
    }

    @Test
    fun `a power waiting for a villager does not wake up when a werewolf dies`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true))
        val waiting = give(
            1,
            role("Crow", powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.ONE_VILLAGER)
        )

        RoleRoster.kill(GameState.seats[0])

        assertEquals(PowerState.CONDITIONAL, waiting.powerState)
    }

    @Test
    fun `a power that waits for the morning wakes up at dawn`() {
        val role = give(
            0,
            role(
                "Hunter",
                powerState = PowerState.CONDITIONAL,
                condition = ConditionalActivation.ON_MORNING,
                isNocturnal = false
            )
        )

        PowerRules.onMorning()

        assertEquals(PowerState.PERMANENT, role.powerState)
    }

    @Test
    fun `the elder power takes the powers of the villagers away`() {
        val villager = give(0, role("Seer", isNocturnal = true))
        val wolf = give(1, role("Werewolf", isWerewolf = true, isNocturnal = true, definesPackTurn = true))

        PowerRules.removeVillagersPowers()

        assertFalse("the villagers lose their night", villager.isNocturnal)
        assertEquals(PowerState.PERMANENT, villager.powerState)
        assertTrue("the werewolves keep theirs", wolf.isNocturnal)
        assertTrue(GameState.eldersPowerRemoved)
        assertEquals(GameMessage.ELDER_POWER_USED, GameState.history.last().text.message)
    }

    // ── The roles of the game, as the file describes them ──────────

    private fun sister() = role(
        "Sister",
        powerState = PowerState.CONDITIONAL,
        condition = ConditionalActivation.IS_ALONE,
        isNocturnal = true,
        becomes = RoleChange(isNocturnal = false, powerState = PowerState.PERMANENT)
    )

    private fun wildChild() = role(
        "WildChild",
        powerState = PowerState.CONDITIONAL,
        condition = ConditionalActivation.MY_TARGET_DIES,
        doTargeting = true,
        becomes = RoleChange(
            isWerewolf = true,
            isNocturnal = true,
            playsAt = NightSlot.WEREWOLF_TURN
        )
    )

    private fun bigBadWolf() = role(
        "BigBadWolf",
        powerState = PowerState.CONDITIONAL,
        condition = ConditionalActivation.ONE_WEREWOLF,
        isWerewolf = true,
        doKilling = true,
        hasSeparateTurn = true,
        becomes = RoleChange(playsAt = NightSlot.WEREWOLF_TURN, powerState = PowerState.PERMANENT)
    )
}
