package main.werewolfkotlin.game

/***
 * The cycle of the game : sleeping, night, dawn, day, sunset, and sleeping again.
 * Only the state changes here, the screen follows (texts, background, band).
 */
object PhaseCycle {

    /***
     * Go to the following phase and return it
     */
    fun advance(): EnumPhase {
        val state = GameState
        state.inspectedOrder = null
        state.inspected = emptyList()

        state.phase = when (state.phase) {
            EnumPhase.SUNSET -> {
                EffectBlocks.resetPermanentTargets() //New night : the permanent powers forget their targets
                state.stepPos = -1 // Reset index for the next night
                state.activeNow = emptyList()
                state.turn++ //Starting new turn
                PowerRules.onNewTurn() // The roles that wait for this turn
                EnumPhase.SLEEPING
            }

            EnumPhase.SLEEPING -> {
                NightFlow.freeze() // The steps of tonight don't change anymore
                EnumPhase.NIGHT
            }

            EnumPhase.NIGHT -> {
                NightFlow.applyPending() // Changes of status asked during the night are now effective
                PowerRules.onMorning() // The powers that wait for the morning
                state.activeNow = emptyList()
                EnumPhase.DAWN
            }

            EnumPhase.DAWN -> EnumPhase.DAY

            EnumPhase.DAY -> EnumPhase.SUNSET
        }
        return state.phase
    }
}
