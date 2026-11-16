package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import model.CharacterGame
import model.ConditionalActivation
import model.NightSlot
import model.PowerState
import model.RoleChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * The roles with a behaviour of their own, written the way the roles files describe them :
 * the doppleganger, the mother of wolves, the white werewolf, the piper, the necromancer,
 * the serial killer.
 */
class SpecialRolesTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 4)
    }

    // ── A role that copies another one ─────────────────────────────

    @Test
    fun `the doppleganger takes the behaviour of the role it chose, and keeps its own card`() {
        val seer = give(0, role("Seer", name = "Seer", doTargeting = true), atOrder = 110)
        val dopple = give(1, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)

        TableActions.toggleTarget(GameState.seats[0])

        assertEquals("it is still the card of the doppleganger", "Actor", dopple.className)
        assertEquals("Doppleganger", dopple.name)
        assertEquals("Seer : action of Seer", dopple.action)
        assertEquals("it plays when the seer plays", seer.order, dopple.order)
        assertEquals(PowerState.PERMANENT, dopple.powerState)
        assertEquals(Pair("Seer", "Seer"), dopple.copiedFrom)
        assertEquals(GameMessage.ROLE_COPIED, GameState.history.first { it.text.message == GameMessage.ROLE_COPIED }.text.message)
    }

    @Test
    fun `the player the doppleganger copied stays visible, even when the copied role chooses nobody`() {
        give(0, role("Villager", name = "Villager", isNocturnal = false))
        val dopple = give(1, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)

        TableActions.toggleTarget(GameState.seats[0])

        val model = GameState.effectBlocks.single { it.kind == EffectBlock.Kind.MODEL }
        assertEquals(listOf(GameState.seats[0]), model.players.toList())
        assertEquals("Doppleganger → Villager", model.title)
        assertTrue("the copied player is lit during the turn of the doppleganger", model.lights)
        assertTrue("a villager chooses nobody : no block of targets left",
            GameState.effectBlocks.none { it.kind == EffectBlock.Kind.TARGETS })
    }

    @Test
    fun `the copied player is not mixed with the targets of the copied role`() {
        give(0, role("Seer", name = "Seer", doTargeting = true), atOrder = 110)
        val dopple = give(1, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)
        TableActions.toggleTarget(GameState.seats[0])       //copies the seer

        TableActions.toggleTarget(GameState.seats[2])       //now plays as a seer : a target of the night
        EffectBlocks.resetPermanentTargets()                //a new night

        val model = GameState.effectBlocks.single { it.kind == EffectBlock.Kind.MODEL }
        assertEquals("who was copied never goes away", listOf(GameState.seats[0]), model.players.toList())
        assertTrue("the targets of the night do", blockOf(dopple).players.isEmpty())
    }

    @Test
    fun `the doppleganger waits when the chosen player has no role yet`() {
        val dopple = give(1, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)

        TableActions.toggleTarget(GameState.seats[0])

        assertNull(dopple.copiedFrom)
        assertFalse("it can still copy somebody", dopple.changeApplied)
    }

    @Test
    fun `a copied werewolf eats with the pack`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true, doKilling = true), atOrder = 140)
        val dopple = give(1, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)

        TableActions.toggleTarget(GameState.seats[0])
        NightFlow.freeze()

        assertTrue(dopple.isWerewolf)
        assertEquals(2, NightFlow.resolveActive(GameState.werewolfTurn).size)
    }

    @Test
    fun `a copied sister counts as a sister so the real one is not alone`() {
        val first = give(0, sister())
        give(1, sister())
        val dopple = give(2, doppleganger(), atOrder = 20)
        GameState.activeNow = listOf(dopple)
        TableActions.toggleTarget(GameState.seats[0])

        RoleRoster.kill(GameState.seats[1])

        assertTrue("two players still act as sisters", first.isNocturnal)
        assertEquals("Sister", dopple.actsAs)
    }

    // ── A role that waits for a turn ───────────────────────────────

    @Test
    fun `the mother of wolves has her own turn the first two nights only`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 140)
        val mother = give(
            1,
            role(
                "PowerfulMotherOfWolves", isWerewolf = true, hasSeparateTurn = true,
                powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.TURN_REACHED,
                becomes = RoleChange(playsAt = NightSlot.WEREWOLF_TURN, powerState = PowerState.PERMANENT)
            ).also { it.conditionTurn = 3 },
            atOrder = 150
        )
        mother.conditionTurn = 3

        startNight()            //night 1
        assertEquals(listOf(140, 150), GameState.nightSteps)
        finishTurn(); startNight()   //night 2
        assertEquals(listOf(140, 150), GameState.nightSteps)
        finishTurn(); startNight()   //night 3

        assertEquals("she only eats with the pack now", listOf(140), GameState.nightSteps)
        assertEquals(NightSlot.WEREWOLF_TURN, mother.playsAt)
    }

    @Test
    fun `the white werewolf loses his own turn when he is the last werewolf`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 140)
        val white = give(
            1,
            role(
                "WhiteWerewolf", isWerewolf = true, hasSeparateTurn = true,
                powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.ALL_WEREWOLVES,
                becomes = RoleChange(playsAt = NightSlot.WEREWOLF_TURN, powerState = PowerState.PERMANENT)
            ),
            atOrder = 200
        )

        RoleRoster.kill(GameState.seats[0])

        assertEquals("nobody left to eat among the wolves", NightSlot.WEREWOLF_TURN, white.playsAt)
    }

    // ── The players a role chose ───────────────────────────────────

    @Test
    fun `the charmed players of the piper stay charmed night after night`() {
        val piper = give(0, role("Piper", name = "Piper", doTargeting = true))
        piper.keepsTargets = true
        give(1, role("Seer", name = "Seer", doTargeting = true))
        blockOf(piper).players.add(GameState.seats[2])
        GameState.effectBlocks.first { it.title == "Seer" }.players.add(GameState.seats[3])

        EffectBlocks.resetPermanentTargets()

        assertEquals(1, blockOf(piper).players.size)
        assertTrue("the seer starts again every night", GameState.effectBlocks.first { it.title == "Seer" }.players.isEmpty())
    }

    @Test
    fun `the necromancer chooses among the dead, never among the living`() {
        val necromancer = give(0, role("Servant", name = "Necromancer", doTargeting = true))
        necromancer.targetsDead = true
        give(1, role("Seer"))
        RoleRoster.kill(GameState.seats[1])
        GameState.activeNow = listOf(necromancer)

        assertNull("a living player cannot be chosen", TableActions.toggleTarget(GameState.seats[2]))

        val said = TableActions.toggleTarget(GameState.seats[1])
        assertEquals(GameMessage.TARGET, said?.message)
        assertEquals(listOf(GameState.seats[1]), blockOf(necromancer).players.toList())
    }

    @Test
    fun `a role that chooses the living cannot choose a dead player`() {
        val seer = give(0, role("Seer", doTargeting = true))
        give(1, role("Villager", isNocturnal = false))
        RoleRoster.kill(GameState.seats[1])
        GameState.activeNow = listOf(seer)

        assertNull(TableActions.toggleTarget(GameState.seats[1]))
    }

    @Test
    fun `the serial killer wakes up to kill when the werewolves are dead, and keeps his list`() {
        give(0, role("Werewolf", isWerewolf = true, definesPackTurn = true), atOrder = 140)
        val killer = give(
            1,
            role(
                "Manipulator", name = "The Serial Killer", isSolo = true, doTargeting = true,
                powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.ALL_WEREWOLVES,
                becomes = RoleChange(isNocturnal = true, doKilling = true, powerState = PowerState.PERMANENT)
            )
        )
        killer.keepsTargets = true
        killer.isNocturnal = false                  //he chose his enemies the first night, then sleeps
        blockOf(killer).players.add(GameState.seats[2])

        RoleRoster.kill(GameState.seats[0])
        EffectBlocks.resetPermanentTargets()

        assertTrue(killer.isNocturnal)
        assertTrue(killer.doKilling)
        assertEquals("his list of enemies is still there", 1, blockOf(killer).players.size)

        //A tap is now a victim
        GameState.activeNow = listOf(killer)
        assertEquals(GameMessage.DEATH_TARGET, TableActions.toggleKill(GameState.seats[3])?.message)
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun doppleganger() = role(
        "Actor", name = "Doppleganger", source = "AUTHOR", doTargeting = true,
        powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.TARGET_CHOSEN,
        becomes = RoleChange(copiesTarget = true)
    )

    private fun sister() = role(
        "Sister", powerState = PowerState.CONDITIONAL, condition = ConditionalActivation.IS_ALONE,
        becomes = RoleChange(isNocturnal = false, powerState = PowerState.PERMANENT)
    )

    private fun blockOf(role: CharacterGame): EffectBlock =
        GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS && it.roleKey == model.kindOf(role) }

    /** From the evening to the night : the roles of tonight are fixed */
    private fun startNight() {
        PhaseCycle.advance()    //sleeping
        PhaseCycle.advance()    //night
    }

    /** From the night to the next evening */
    private fun finishTurn() {
        PhaseCycle.advance()    //dawn
        PhaseCycle.advance()    //day
        PhaseCycle.advance()    //sunset
    }
}
