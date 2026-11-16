package main.werewolfkotlin.game

import model.CharacterGame
import model.ConditionalActivation
import model.PowerState
import model.RoleChange
import model.kindOf

/***
 * Roles that change because of what happens in the game.
 *
 * A role says what has to happen (its condition) and what it becomes then (its change).
 * This file only watches the game and applies what the role describes : no role is named here.
 *
 * The game is watched at four moments, one function each :
 *   afterDeath     somebody died
 *   afterTargeting a role has just chosen a player
 *   onNewTurn      a new turn starts
 *   onMorning      the night ends
 */
object PowerRules {

    // ── The four moments ───────────────────────────────────────────

    /***
     * Somebody died : every conditional role whose condition is now met changes.
     * [dead] is the role he held, null when he had none, and [deadSeat] is his place at the table.
     */
    fun afterDeath(dead: CharacterGame?, deadSeat: Seat? = null) {
        val alive = GameState.aliveCharacters

        for (role in alive.toList()) {
            if (!canChange(role)) continue

            val met = when (role.condition) {
                //A player who held no role was neither a werewolf nor a villager for these two
                ConditionalActivation.ONE_WEREWOLF -> dead != null && dead.isWerewolf
                ConditionalActivation.ONE_VILLAGER -> dead != null && !dead.isWerewolf
                //Every other werewolf : the white werewolf waits to be the last one
                ConditionalActivation.ALL_WEREWOLVES -> alive.none { it !== role && it.isWerewolf }
                ConditionalActivation.ALL_VILLAGERS -> alive.filter { it !== role }.all { it.isWerewolf }
                ConditionalActivation.IS_ALONE -> alive.count { it.actsAs == role.actsAs } == 1
                ConditionalActivation.MY_TARGET_DIES -> deadSeat != null && hasChosen(role, deadSeat)
                //The other conditions do not wait for a death
                else -> false
            }
            if (met) apply(role, null)
        }
    }

    /***
     * The role of this block has just chosen a player
     */
    fun afterTargeting(block: EffectBlock, chosen: Seat) {
        val owners = GameState.aliveCharacters.filter { kindOf(it) == block.roleKey }

        for (role in owners) {
            if (role.condition == ConditionalActivation.TARGET_CHOSEN && canChange(role)) apply(role, chosen)
        }
    }

    /***
     * A new turn starts : the roles that wait for this turn change
     */
    fun onNewTurn() {
        for (role in GameState.aliveCharacters.toList()) {
            if (role.condition != ConditionalActivation.TURN_REACHED || !canChange(role)) continue
            if (GameState.turn >= role.conditionTurn) apply(role, null)
        }
    }

    /***
     * Morning : the roles that wait for it change
     */
    fun onMorning() {
        for (role in GameState.aliveCharacters.toList()) {
            if (role.condition == ConditionalActivation.ON_MORNING && canChange(role)) apply(role, null)
        }
    }

    /***
     * The elder power : the special villagers (not solo, not werewolves, the infected don't count)
     * lose their powers. This one changes other roles than its own, so it stays a rule of its own.
     */
    fun removeVillagersPowers() {
        GameState.aliveCharacters.filter { !it.isSolo && !it.isWerewolf }.forEach { role ->
            role.isNocturnal = false
            role.powerState = PowerState.PERMANENT
        }
        GameState.eldersPowerRemoved = true
        GameState.log(GameMessage.ELDER_POWER_USED)
    }

    // ── Can it change ? ────────────────────────────────────────────

    /***
     * A role can still change : it is conditional, and its change did not already happen
     */
    private fun canChange(role: CharacterGame): Boolean {
        //Only a conditional role waits for something
        if (role.powerState != PowerState.CONDITIONAL) return false

        //Already changed : it only changes again when its change says so
        if (role.changeApplied) return role.becomes?.fires == RoleChange.Firing.EVERY_TIME

        return true
    }

    /***
     * This role had chosen the player of this place (its block of targets holds him)
     */
    private fun hasChosen(role: CharacterGame, seat: Seat): Boolean {
        return GameState.effectBlocks.any { block ->
            block.kind == EffectBlock.Kind.TARGETS
                    && block.roleKey == kindOf(role)
                    && block.players.any { it === seat }
        }
    }

    // ── Applying the change ───────────────────────────────────────

    /***
     * The role changes : what its file describes, or waking up again when it describes nothing.
     * [chosen] is the player the role has just chosen, when that is what triggered the change.
     */
    private fun apply(role: CharacterGame, chosen: Seat?) {
        val change = role.becomes

        if (change == null) {
            //Nothing described : the role comes back to the night state it had at its creation
            role.isNocturnal = role.originalNightState
            role.powerState = PowerState.PERMANENT
            role.changeApplied = true
        } else if (change.copiesTarget) {
            //Nothing to copy yet : the chosen player has no role, the change waits for another choice
            val model = chosen?.character ?: return
            if (model === role) return
            copy(role, model, chosen)
        } else {
            change.applyTo(role)
            role.changeApplied = true
        }

        //The role may play elsewhere, or choose players now : the game follows
        EffectBlocks.sync()
        RoleRoster.updateWerewolfTurn()
    }

    /***
     * The role takes the behaviour of the role of the chosen player, and plays at the same moment.
     * If the copied role waits for something itself, the copy now waits for the same thing.
     */
    private fun copy(role: CharacterGame, model: CharacterGame, chosen: Seat) {
        role.copyBehaviourOf(model)
        role.order = model.order
        role.changeApplied = false

        if (role.powerState == PowerState.CONSUMABLE) GameState.consumables[role] = false

        //Who was copied stays visible for the whole game
        EffectBlocks.keepModel(role, chosen, role.name + " → " + model.name)

        GameState.log(GameMessage.ROLE_COPIED, role.name, model.name, chosen.name)
    }
}
