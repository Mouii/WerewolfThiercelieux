package main.werewolfkotlin.game

import GameFixtures.give
import GameFixtures.newGame
import GameFixtures.role
import model.NightSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/***
 * The moment of the pack of werewolves, and the roles that play with it
 */
class PackTurnTest {

    @Before
    fun emptyGame() {
        newGame(playerCount = 3)
    }

    @Test
    fun `the role that carries the pack gives it its moment`() {
        give(0, werewolf(), atOrder = 140)

        assertEquals(140, GameState.werewolfTurn)
    }

    @Test
    fun `a role that plays with the pack follows it, whatever the order the roles were given in`() {
        //The wolfhound arrives first, when there is no pack yet
        val hound = give(0, role("WolfHound", playsAt = NightSlot.WEREWOLF_TURN), atOrder = 160)
        give(1, werewolf(), atOrder = 140)

        assertEquals("the pack keeps its own moment", 140, GameState.werewolfTurn)
        assertEquals("the wolfhound joined it", 140, hound.order)
    }

    @Test
    fun `a role that plays with the pack is given its moment right away`() {
        give(0, werewolf(), atOrder = 140)
        val girl = give(1, role("LittleGirl", playsAt = NightSlot.WEREWOLF_TURN), atOrder = 170)

        assertEquals(140, girl.order)
    }

    @Test
    fun `a role placed on the pack is only called with the pack`() {
        give(0, werewolf(), atOrder = 140)
        give(1, role("LittleGirl", playsAt = NightSlot.WEREWOLF_TURN), atOrder = 170)
        NightFlow.freeze()

        assertEquals("one single moment for both", listOf(140), GameState.nightSteps)
        assertEquals(2, NightFlow.resolveActive(140).size)
    }

    @Test
    fun `a role that has a turn of its own does not make a pack by itself`() {
        give(0, role("BigBadWolf", isWerewolf = true, hasSeparateTurn = true), atOrder = 180)
        GameState.wolfTurnForced = true
        RoleOrders.set("Werewolf", 140)
        RoleRoster.updateWerewolfTurn()

        assertTrue("nobody to eat with", NightFlow.resolveActive(140).isEmpty())
    }

    @Test
    fun `two different roles never share a moment by accident`() {
        //Two roles the application does not know : they would land on the same place
        val first = give(0, role("MyOwnRole"))
        val second = give(1, role("AnotherOwnRole"))

        RoleOrders.set("MyOwnRole", 400)
        RoleOrders.set("AnotherOwnRole", 400)
        RoleRoster.setRole(GameState.seats[0], null)
        RoleRoster.setRole(GameState.seats[1], null)

        val a = give(0, role("MyOwnRole"))
        val b = give(1, role("AnotherOwnRole"))

        assertTrue("they must be called one after the other", a.order != b.order)
        assertEquals(400, minOf(a.order, b.order))
        assertEquals(401, maxOf(a.order, b.order))
        assertTrue(first.className != second.className)
    }

    // ── The pack without wolves ────────────────────────────────────

    @Test
    fun `a game without any werewolf role still calls the pack`() {
        GameState.wolfTurnForced = true
        give(0, role("Seer"), atOrder = 110)
        RoleOrders.set("Werewolf", 140)
        RoleRoster.updateWerewolfTurn()

        NightFlow.freeze()

        assertEquals(140, GameState.werewolfTurn)
        assertEquals("the seer, then the pack nobody holds", listOf(110, 140), GameState.nightSteps)
        assertTrue(NightFlow.isPackWithoutWolves(140))
    }

    @Test
    fun `the pack is called even when no role at all was given`() {
        GameState.wolfTurnForced = true
        RoleOrders.set("Werewolf", 140)
        RoleRoster.updateWerewolfTurn()

        NightFlow.freeze()

        assertEquals(listOf(140), GameState.nightSteps)
    }

    @Test
    fun `the pack without wolves says what happens and takes a victim`() {
        GameState.wolfTurnForced = true
        RoleOrders.set("Werewolf", 140)
        RoleRoster.updateWerewolfTurn()
        EffectBlocks.sync()
        NightFlow.freeze()
        GameState.phase = EnumPhase.NIGHT
        NightFlow.next()

        val call = NightFlow.enter(140)
        val said = TableActions.toggleKill(GameState.seats[1])

        assertEquals("Werewolves", call.roles.single().roleName)
        assertEquals(GameMessage.DEATH_TARGET, said?.message)
        assertEquals(listOf("Werewolves", "Player 2"), said?.names)
    }

    @Test
    fun `the pack without wolves disappears when a role that kills is given`() {
        GameState.wolfTurnForced = true
        RoleOrders.set("Werewolf", 140)
        give(0, role("Seer"), atOrder = 110)
        assertTrue(NightFlow.isPackWithoutWolves(GameState.werewolfTurn))

        //A wolf that has its own turn : alone, it does not make a pack, and the false one is gone
        give(1, role("BigBadWolf", isWerewolf = true, doKilling = true, hasSeparateTurn = true), atOrder = 180)
        NightFlow.freeze()

        assertTrue(!NightFlow.isPackWithoutWolves(GameState.werewolfTurn))
        assertEquals(listOf(110, 180), GameState.nightSteps)
    }

    @Test
    fun `the moderator can turn the pack without wolves off`() {
        GameState.wolfTurnForced = false
        give(0, role("Seer"), atOrder = 110)

        RoleRoster.updateWerewolfTurn()
        NightFlow.freeze()

        assertEquals(-1, GameState.werewolfTurn)
        assertEquals(listOf(110), GameState.nightSteps)
    }

    private fun werewolf() = role("Werewolf", isWerewolf = true, definesPackTurn = true, doKilling = true)
}
