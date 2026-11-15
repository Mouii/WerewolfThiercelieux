package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/***
 * The cycle of a turn : the village sleeps, the night, the morning, the day, the evening
 */
class PhaseCycleTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 2)
    }

    @Test
    fun `the phases follow each other and a new turn starts in the evening`() {
        give(0, role("Seer"), atOrder = 10)
        assertEquals(EnumPhase.SUNSET, GameState.phase)
        assertEquals(0, GameState.turn)

        assertEquals(EnumPhase.SLEEPING, PhaseCycle.advance())
        assertEquals("the turn starts when the village goes to sleep", 1, GameState.turn)

        assertEquals(EnumPhase.NIGHT, PhaseCycle.advance())
        assertEquals(EnumPhase.DAWN, PhaseCycle.advance())
        assertEquals(EnumPhase.DAY, PhaseCycle.advance())
        assertEquals(EnumPhase.SUNSET, PhaseCycle.advance())

        assertEquals(EnumPhase.SLEEPING, PhaseCycle.advance())
        assertEquals(2, GameState.turn)
    }

    @Test
    fun `the roles called tonight are fixed when the night starts`() {
        val seer = give(0, role("Seer"), atOrder = 10)
        PhaseCycle.advance()        //sleeping
        PhaseCycle.advance()        //night

        assertEquals(listOf(10), GameState.nightSteps)

        //A role put to sleep during the night does not change what is still to be called
        seer.isNocturnal = false
        assertEquals(listOf(10), GameState.nightSteps)
    }

    @Test
    fun `a permanent power forgets its targets when a new night starts`() {
        give(0, role("Defender", doTargeting = true, powerState = model.PowerState.PERMANENT))
        val block = GameState.effectBlocks.first { it.kind == EffectBlock.Kind.TARGETS }
        block.players.add(GameState.seats[1])

        PhaseCycle.advance()        //sleeping : a new night is being prepared

        assertEquals(0, block.players.size)
    }

    @Test
    fun `a power that waits for the morning wakes up at dawn`() {
        val role = give(
            0,
            role(
                "Hunter",
                powerState = model.PowerState.CONDITIONAL,
                condition = model.ConditionalActivation.ON_MORNING,
                isNocturnal = false
            )
        )

        GameState.phase = EnumPhase.NIGHT
        PhaseCycle.advance()        //dawn

        assertEquals(model.PowerState.PERMANENT, role.powerState)
    }
}
