package main.werewolfkotlin.game

import model.CharacterGame
import model.RoleId

/***
 * The night : which roles are called, in which order, and the moves of the moderator in it
 * (next, previous, jump to a role, swap two moments, wake up / put to sleep).
 */
object NightFlow {

    // ── Who plays ──────────────────────────────────────────────────

    /***
     * Who plays at this order.
     * The result can be empty, or only made of sleeping characters : the step is then skipped.
     */
    fun resolveActive(index: Int): List<CharacterGame> {
        val alive = GameState.aliveCharacters
        var active: List<CharacterGame>

        if (index == GameState.werewolfTurn) {
            //The pack of werewolves, plus the infected players who wake up with it
            active = alive.filter { x -> (x.isWerewolf && x.isNocturnal) || x.isInfected }

            //If there is only one werewolf at this turn, better skip it
            if (active.size == 1 && (active[0].className == RoleId.BIG_BAD_WOLF
                        || active[0].className == RoleId.WHITE_WEREWOLF))
                active = emptyList()

        } else {
            active = alive.filter { x -> x.order == index }

            //Special cases : if in the unity there are at the same time sleeping roles and awaken roles
            //We filter the non awaken roles
            if (active.any { it.isNocturnal } && active.any { !it.isNocturnal })
                active = active.filter { it.isNocturnal }
        }

        return active
    }

    /***
     * Somebody is awake at this step. The pack of werewolves is always awake, infected included
     */
    fun isStepAwake(index: Int, active: List<CharacterGame>): Boolean {
        return active.isNotEmpty() && (index == GameState.werewolfTurn || active.all { it.isNocturnal })
    }

    fun isAwakeNow(order: Int): Boolean = isStepAwake(order, resolveActive(order))

    /***
     * Characters shown in the band entry of this order
     */
    fun membersOf(order: Int): List<CharacterGame> {
        val alive = GameState.aliveCharacters
        val live = if (order == GameState.werewolfTurn)
            alive.filter { it.order == order || (it.isWerewolf && it.isNocturnal && !it.isInfected) }
        else
            alive.filter { it.order == order }

        return live.ifEmpty { GameState.nightActive[order].orEmpty() }
    }

    // ── Steps ──────────────────────────────────────────────────────

    /***
     * Freeze tonight's steps : only the ones where somebody is awake
     */
    fun freeze() {
        val alive = GameState.aliveCharacters
        val maxKey: Int = alive.maxOfOrNull { x -> x.order } ?: -1

        val steps = mutableListOf<Int>()
        val actives = mutableMapOf<Int, List<CharacterGame>>()
        for (index in 0..maxKey) {
            val active = resolveActive(index)
            if (isStepAwake(index, active)) {
                steps.add(index)
                actives[index] = active
            }
        }

        GameState.nightSteps = steps
        GameState.nightActive = actives
        GameState.stepPos = -1
        GameState.calledOrders.clear()
        GameState.stepCalls.clear()
        GameState.activeNow = emptyList()

        GameState.pendingNocturnal.clear()
        GameState.nightSnapshot.clear()
        alive.forEach { GameState.nightSnapshot[it] = it.isNocturnal }
    }

    /** Next role of the night : its order. Null after the last one, the night ends */
    fun next(): Int? {
        if (GameState.stepPos + 1 >= GameState.nightSteps.size) return null
        GameState.stepPos++
        return GameState.nightSteps[GameState.stepPos]
    }

    /** Previous role of the night : its order. Null if there is none */
    fun previous(): Int? {
        if (GameState.phase != EnumPhase.NIGHT || GameState.stepPos <= 0) return null
        GameState.stepPos--
        return GameState.nightSteps[GameState.stepPos]
    }

    /** Call (or call again) a precise role. False if it is not called tonight */
    fun jumpTo(order: Int): Boolean {
        val position = GameState.nightSteps.indexOf(order)
        if (position < 0) return false
        GameState.stepPos = position
        return true
    }

    /***
     * Call a step of the night : what the moderator has to say.
     * The effects of a role (unique power, wild child) only happen the first time the step is called.
     */
    fun enter(order: Int): NightCall {
        val actives = GameState.nightActive[order].orEmpty()

        if (GameState.calledOrders.add(order)) {
            if (actives.size == 1 && actives[0].className == RoleId.WILD_CHILD) {
                PowerRules.setupWildChild(actives[0])
            }
            GameState.stepCalls[order] = describe(order, actives)
        }

        GameState.activeNow = actives
        return GameState.stepCalls[order] ?: NightCall(emptyList())
    }

    /***
     * What is said at this step : one line per role called, and the infected players of the pack.
     * Calling the action of a role can change it (a power used only once), so this is done once a night.
     */
    private fun describe(order: Int, characters: List<CharacterGame>): NightCall {
        val isWolfTurn = order == GameState.werewolfTurn

        //Infected players only follow the pack, their own role is called at its own turn
        val shown = if (isWolfTurn) characters.filter { !it.isInfected } else characters

        val lines = shown.distinctBy { it.name }.map { NightCall.CalledRole(it.name, it.nightAction()) }

        val infected = if (isWolfTurn)
            GameState.seats.filter { it.alive && it.character?.isInfected == true }.map { it.name }
        else
            emptyList()

        return NightCall(lines, infected)
    }

    /***
     * Look at a role outside of the night : who has it, and its red and green lights. Same role again to stop
     */
    fun inspect(order: Int) {
        if (GameState.inspectedOrder == order) {
            GameState.inspectedOrder = null
            GameState.inspected = emptyList()
        } else {
            GameState.inspectedOrder = order
            GameState.inspected = membersOf(order)
        }
    }

    // ── Turn order ─────────────────────────────────────────────────

    /***
     * Swap two moments of the turn order. The new places are kept in RoleOrders :
     * the caller saves them (data.RoleOrderStore) for the next games.
     */
    fun swapOrders(orderA: Int, orderB: Int) {
        val alive = GameState.aliveCharacters
        val first = alive.filter { it.order == orderA }
        val second = alive.filter { it.order == orderB }
        first.forEach { it.order = orderB }
        second.forEach { it.order = orderA }

        //The pack of werewolves follows its order
        if (GameState.werewolfTurn == orderA) GameState.werewolfTurn = orderB
        else if (GameState.werewolfTurn == orderB) GameState.werewolfTurn = orderA

        first.forEach { RoleOrders.set(it.className, it.order) }
        second.forEach { RoleOrders.set(it.className, it.order) }
        if (alive.none { it.className == RoleId.WEREWOLF } && GameState.werewolfTurn >= 0) {
            RoleOrders.set(RoleId.WEREWOLF, GameState.werewolfTurn)
        }

        //During the night, the steps of the night follow : the role that is called stays the called one
        if (GameState.phase == EnumPhase.NIGHT) {
            fun moved(order: Int) = when (order) {
                orderA -> orderB
                orderB -> orderA
                else -> order
            }

            val current = GameState.nightSteps.getOrNull(GameState.stepPos)
            GameState.nightSteps = GameState.nightSteps.map { moved(it) }.sorted()
            GameState.nightActive = GameState.nightActive.mapKeys { moved(it.key) }

            val called = GameState.calledOrders.map { moved(it) }
            GameState.calledOrders.clear()
            GameState.calledOrders.addAll(called)

            val calls = GameState.stepCalls.mapKeys { moved(it.key) }
            GameState.stepCalls.clear()
            GameState.stepCalls.putAll(calls)

            if (current != null) GameState.stepPos = GameState.nightSteps.indexOf(moved(current))
        }

        alive.sortBy { it.order }
    }

    // ── Awake / asleep ─────────────────────────────────────────────

    /***
     * Wake up / put to sleep some roles at once (one line in the historic).
     * During the night, the change is only applied when the night ends.
     */
    fun setAwakeGroup(members: List<CharacterGame>, awake: Boolean, label: String) {
        val state = GameState
        var changed = false
        for (c in members) {
            if (state.phase == EnumPhase.NIGHT) {
                val base = state.nightSnapshot[c] ?: c.isNocturnal
                val shown = state.pendingNocturnal[c] ?: base
                if (shown == awake) continue
                if (awake == base) state.pendingNocturnal.remove(c) else state.pendingNocturnal[c] = awake
                changed = true
            } else if (c.isNocturnal != awake) {
                c.isNocturnal = awake
                changed = true
            }
        }
        if (!changed) return

        val message = if (state.phase == EnumPhase.NIGHT) {
            if (awake) GameMessage.ROLES_AWAKE_PENDING else GameMessage.ROLES_ASLEEP_PENDING
        } else {
            if (awake) GameMessage.ROLES_AWAKE else GameMessage.ROLES_ASLEEP
        }
        state.log(message, label)
    }

    /** The night is over : the changes asked during the night are now effective */
    fun applyPending() {
        GameState.pendingNocturnal.forEach { (character, awake) -> character.isNocturnal = awake }
        GameState.pendingNocturnal.clear()
    }
}
