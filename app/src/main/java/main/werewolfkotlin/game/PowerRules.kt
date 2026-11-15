package main.werewolfkotlin.game

import model.CharacterGame
import model.ConditionalActivation
import model.PowerState
import model.RoleId
import model.stickKeyOf

/***
 * Powers that wake up (or change) because of what happens in the game
 */
object PowerRules {

    /***
     * Activate the special conditions after a death
     */
    fun afterDeath(dead: CharacterGame) {
        val alive = GameState.aliveCharacters
        alive.filter { it.powerState == PowerState.CONDITIONAL }.forEach { x ->
            when (x.condition) {
                ConditionalActivation.LINKED_PLAYERS -> {
                    if (alive.none { x.rolesToStick.contains(stickKeyOf(it)) }) activate(x)
                }

                ConditionalActivation.ONE_WEREWOLF -> {
                    if (dead.isWerewolf) {
                        //Unique special case for big bad wolf
                        if (x.className == RoleId.BIG_BAD_WOLF && x.mode == "NORMAL" && GameState.werewolfTurn >= 0) {
                            x.order = GameState.werewolfTurn
                            x.powerState = PowerState.PERMANENT
                        } else {
                            activate(x)
                        }
                    }
                }

                ConditionalActivation.ONE_VILLAGER -> {
                    if (!dead.isWerewolf) activate(x)
                }

                ConditionalActivation.ALL_WEREWOLVES -> {
                    if (alive.none { it.isWerewolf }) activate(x)
                }

                ConditionalActivation.ALL_VILLAGERS -> {
                    if (alive.filter { it != x }.all { it.isWerewolf }) activate(x)
                }

                ConditionalActivation.IS_ALONE -> {
                    if (alive.count { it.className == x.className } == 1) activate(x)
                }

                ConditionalActivation.NO_CONDITION -> {}
                //Activated when the morning comes, not when somebody dies (see onMorning)
                ConditionalActivation.ON_MORNING -> {}
            }
        }
    }

    /***
     * Morning : the roles with the condition ON_MORNING activate their power
     */
    fun onMorning() {
        GameState.aliveCharacters
            .filter { it.powerState == PowerState.CONDITIONAL && it.condition == ConditionalActivation.ON_MORNING }
            .forEach { activate(it) }
    }

    /***
     * The elder power : the special villagers (not solo, not werewolves, the infected don't count)
     * lose their powers
     */
    fun removeVillagersPowers() {
        GameState.aliveCharacters.filter { !it.isSolo && !it.isWerewolf }.forEach { x ->
            x.isNocturnal = false
            x.powerState = PowerState.PERMANENT
        }
        GameState.eldersPowerRemoved = true
        GameState.log(GameMessage.ELDER_POWER_USED)
    }

    /***
     * Back to the night state it had at its creation, and the power becomes permanent
     */
    private fun activate(character: CharacterGame) {
        character.isNocturnal = character.originalNightState
        character.powerState = PowerState.PERMANENT
    }

    /***
     * Setup the wild child for the turn of werewolf but not awaken
     */
    fun setupWildChild(child: CharacterGame) {
        if (GameState.werewolfTurn >= 0) child.order = GameState.werewolfTurn
        child.isWerewolf = true
        child.isNocturnal = false
    }
}
