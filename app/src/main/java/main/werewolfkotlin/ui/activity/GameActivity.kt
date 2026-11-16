package main.werewolfkotlin.ui.activity

import main.werewolfkotlin.ui.Narrator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.R
import main.werewolfkotlin.data.ExtensionSettings
import main.werewolfkotlin.data.GameSaveStore
import main.werewolfkotlin.data.GameSnapshot
import main.werewolfkotlin.data.RoleOrderStore
import main.werewolfkotlin.data.RolesRepository
import main.werewolfkotlin.databinding.ActivityGameBinding
import main.werewolfkotlin.game.EffectBlock
import main.werewolfkotlin.game.EnumPhase
import main.werewolfkotlin.game.GameSession
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.Lights
import main.werewolfkotlin.game.NightFlow
import main.werewolfkotlin.game.PhaseCycle
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import main.werewolfkotlin.game.TableActions
import main.werewolfkotlin.ui.BandEntries
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.setTextFitting
import main.werewolfkotlin.ui.dialog.RandomRolesDialog
import main.werewolfkotlin.ui.dialog.RoleDescriptionDialog
import main.werewolfkotlin.ui.dialog.SeatDialog
import main.werewolfkotlin.ui.dialog.TurnOrderDialog
import main.werewolfkotlin.ui.dialog.confirmDialog

/***
 * The game screen : the players around the table, the turn order band, the main button.
 * The rules are in the game package, the windows in ui.dialog : this screen only links them.
 */
class GameActivity : AppCompatActivity() {

    companion object {
        /** Names of the players chosen in the game setup (an empty name is the default "Player N") */
        const val EXTRA_PLAYER_NAMES = "PlayerNames"

        /** Back to a game that was left : the setup page offers to resume it */
        const val EXTRA_RESUME = "Resume"
    }

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityGameBinding

    //The game is over for good (end button) : nothing to resume
    private var discardSave: Boolean = false

    //What the windows ask to the screen
    private val seatEvents = SeatDialog.Events(
        onChanged = { refreshUi() },
        onGameOver = { showGameOver() },
        onReopened = { binding.startNightButton.isEnabled = true; setStatusText("") }
    )

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startOrResumeGame(savedInstanceState)
        bindButtons()
        bindTable()

        updateHeader()
        refreshUi()
    }

    /***
     * The game of this screen : the one that was left, or a new one with the players of the setup page
     */
    private fun startOrResumeGame(savedInstanceState: Bundle?) {
        GameState.reset()

        //The rules never hold a text of the application : the screen gives them the few labels they need
        GameState.labels = Narrator.labels(this)

        setStatusText(getString(R.string.GameView_StartGame))
        setBackground(GameState.phase)

        //Players chosen in the game setup (an empty name is the default "Player N")
        val names: List<String> = intent.getStringArrayListExtra(EXTRA_PLAYER_NAMES) ?: arrayListOf()

        //A game is resumed when the setup page asked for it, and whenever this screen is built again
        //after Android destroyed it (low memory). In that second case the intent is still the one of the
        //first start, so the saved game - not the intent - says what the moderator was playing.
        val resume = intent.getBooleanExtra(EXTRA_RESUME, false) || savedInstanceState != null
        val saved = if (resume) GameSaveStore.read(this) else null

        if (saved != null && GameSnapshot.restore(this, saved)) {
            showRestoredGame()
        } else if (names.isNotEmpty()) {
            RoleOrderStore.load(this)
            GameSession.start(names, ExtensionSettings.availableRoles(this))
        } else {//Nothing to play : no player given, and no game to resume
            GameState.inProgress = false
        }
    }

    /***
     * The buttons of the screen
     */
    private fun bindButtons() {
        binding.startNightButton.setOnClickListener { onMainButton() }
        binding.prevButton.setOnClickListener { goPrev() }
        binding.randomButton.setOnClickListener { RandomRolesDialog(this) { refreshUi() }.show() }

        binding.resetRolesButton.setOnClickListener {
            confirmDialog(this, getString(R.string.ResetRoles_Title), getString(R.string.ResetRoles_Confirm)) { resetRoles() }
        }
        binding.restartButton.setOnClickListener {
            confirmDialog(this, getString(R.string.Restart_Title), getString(R.string.Restart_Confirm)) { restartGame() }
        }

        binding.deathsSwitch.setOnCheckedChangeListener { _, checked ->
            GameState.showDeaths = checked
            refreshUi()
        }

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            //The game is over : nothing to resume
            discardSave = true
            GameSaveStore.clear(this)
            finish()
        }

        binding.InformationButton.setOnClickListener {
            startActivity(Intent(this, InformationActivity::class.java))
        }
        binding.RoleButton.setOnClickListener {
            startActivity(Intent(this, RoleActivity::class.java))
        }
    }

    /***
     * The players around the table, and the band of the turn order
     */
    private fun bindTable() {
        //Tap on a player : name, role, death
        binding.playerCircle.onSeatTapped = { index -> onSeatTap(index) }
        binding.playerCircle.onSeatLongPressed = { index -> SeatDialog(this, seatEvents).show(index) }

        //Turn order band
        binding.turnBand.onSelect = { order -> jumpTo(order) }
        binding.turnBand.onDescribe = { order -> RoleDescriptionDialog.show(this, order) }
        binding.turnBand.onToggleAwake = { order -> toggleAwake(order) }
        binding.turnBand.onOpenOrder = { showTurnOrderDialog() }
    }

    /***
     * Back from the other screens : roles, infected players... may have changed
     */
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refreshUi()
    }

    /***
     * Leaving the game (back, home, another application) keeps everything, to resume it later
     */
    override fun onPause() {
        super.onPause()
        saveGame()
    }

    /***
     * Override of destroying
     */
    override fun onDestroy() {
        super.onDestroy()
        GameState.reset()
    }

    // ──────────────────────────────────────────────────────────────
    //  Save and resume
    // ──────────────────────────────────────────────────────────────

    private fun saveGame() {
        //An empty table means the game could not be built : the save of the real game must stay
        if (discardSave || !::binding.isInitialized || GameState.seats.isEmpty()) return
        try {
            GameSaveStore.write(this, GameSnapshot.build())
        } catch (ex: Exception) {
            Log.e("GameActivity", "The game could not be saved", ex)
        }
    }

    /***
     * The game is back as it was : the screen follows
     */
    private fun showRestoredGame() {
        binding.deathsSwitch.isChecked = GameState.showDeaths
        setBackground(GameState.phase)
        setStatusText(GameState.statusText)
        binding.startNightButton.isEnabled = GameState.inProgress
    }

    // ──────────────────────────────────────────────────────────────
    //  Phases and night
    // ──────────────────────────────────────────────────────────────

    /***
     * Main button : next role at night, next phase otherwise
     */
    private fun onMainButton() {
        if (!GameState.inProgress) {
            //Only on bad catch up
            setStatusText(getString(R.string.GameView_BadInit))
        } else if (GameState.phase == EnumPhase.NIGHT) {
            goNext()
        } else {
            nextPhase()
        }
    }

    /***
     * Global function handling all phases
     */
    private fun nextPhase() {
        //The turn changes : the purple players are eliminated
        if (GameState.phase == EnumPhase.DAWN || GameState.phase == EnumPhase.DAY) {
            if (RoleRoster.applyPendingDeaths()) showGameOver()
        }

        val phase = PhaseCycle.advance()
        setBackground(phase)
        updateHeader()

        when (phase) {
            EnumPhase.SLEEPING -> setStatusText(getString(R.string.GameView_phaseSleeping))
            EnumPhase.NIGHT -> {}
            EnumPhase.DAWN -> setStatusText(getString(R.string.GameView_phaseDawn))
            EnumPhase.DAY -> setStatusText(getString(R.string.GameView_phaseDay))
            EnumPhase.SUNSET -> setStatusText(getString(R.string.GameView_phaseEvening))
        }

        //Night : start showing the roles at the first moment
        if (phase == EnumPhase.NIGHT) goNext() else refreshUi()
    }

    /***
     * Next role of the night. After the last one, the night ends
     */
    private fun goNext() {
        val order = NightFlow.next()
        if (order != null) showStep(order) else nextPhase()
    }

    /***
     * Previous role of the night
     */
    private fun goPrev() {
        NightFlow.previous()?.let { showStep(it) }
    }

    /***
     * Call (or call again) a precise role from the band. Outside of the night, look at it instead
     */
    private fun jumpTo(order: Int) {
        if (GameState.phase != EnumPhase.NIGHT) {
            NightFlow.inspect(order)
            refreshUi()
            return
        }
        if (!NightFlow.jumpTo(order)) {
            Toast.makeText(this, R.string.Band_NotCalledTonight, Toast.LENGTH_SHORT).show()
            return
        }
        showStep(order)
    }

    private fun showStep(order: Int) {
        setStatusText(Narrator.say(this, NightFlow.enter(order)))
        refreshUi()
    }

    private fun showGameOver() {
        setStatusText(getString(R.string.GameView_GameOver))
        binding.startNightButton.isEnabled = false
    }

    // ──────────────────────────────────────────────────────────────
    //  Turn order
    // ──────────────────────────────────────────────────────────────

    private fun toggleAwake(order: Int) {
        BandEntries.toggleAwake(this, order)
        refreshUi()
    }

    private fun showTurnOrderDialog() {
        TurnOrderDialog.show(
            this,
            entries = { BandEntries.build(this) },
            onSwap = { a, b -> swapOrders(a, b) },
            onToggleAwake = { order -> toggleAwake(order) },
            onWolfTurnChanged = {
                RoleRoster.updateWerewolfTurn()
                refreshUi()
            }
        )
    }

    /***
     * Swap two moments of the turn order. The new places are kept for the next games
     */
    private fun swapOrders(orderA: Int, orderB: Int): Boolean {
        NightFlow.swapOrders(orderA, orderB)
        RoleOrderStore.save(this)
        refreshUi()
        return true
    }

    // ──────────────────────────────────────────────────────────────
    //  Players
    // ──────────────────────────────────────────────────────────────

    /***
     * Tap on a player :
     * - when a role that kills is called (werewolves) : the player is selected to die, in purple (tap again to cancel)
     * - when another role is called : the player is its target, stored in the block of the role (tap again to remove)
     * - in the morning and during the day : the player is selected to die, in purple (tap again to cancel)
     * - otherwise : his window
     * A long press always opens his window.
     */
    private fun onSeatTap(index: Int) {
        val seat: Seat = GameState.seats[index]

        //A dead player can only be chosen by a called role that chooses among the dead
        if (!seat.alive && GameState.phase == EnumPhase.NIGHT) {
            val target = TableActions.toggleTarget(seat)
            if (target != null) {
                Toast.makeText(this, Narrator.say(this, target), Toast.LENGTH_SHORT).show()
                refreshUi()
                return
            }
        }

        if (seat.alive) {
            if (GameState.phase == EnumPhase.NIGHT) {
                val kill = TableActions.toggleKill(seat)
                if (kill != null) {
                    showDeathMark()
                    Toast.makeText(this, Narrator.say(this, kill), Toast.LENGTH_SHORT).show()
                    refreshUi()
                    return
                }

                val target = TableActions.toggleTarget(seat)
                if (target != null) {
                    Toast.makeText(this, Narrator.say(this, target), Toast.LENGTH_SHORT).show()
                    refreshUi()
                    return
                }
            }

            if (GameState.phase == EnumPhase.DAWN || GameState.phase == EnumPhase.DAY) {
                TableActions.toggleDeath(seat)
                showDeathMark()
                refreshUi()
                return
            }
        }
        SeatDialog(this, seatEvents).show(index)
    }

    /***
     * Purple has to be visible to be useful
     */
    private fun showDeathMark() {
        if (!binding.deathsSwitch.isChecked) binding.deathsSwitch.isChecked = true
    }

    // ──────────────────────────────────────────────────────────────
    //  Restart and reset
    // ──────────────────────────────────────────────────────────────

    /***
     * Start the game again : same players, same roles, but nobody is dead, no target, everybody sleeps
     */
    private fun restartGame() {
        GameSession.restart { className, key -> RolesRepository.prototypeOf(className, key) }

        binding.startNightButton.isEnabled = true
        setBackground(GameState.phase)
        updateHeader()
        setStatusText(getString(R.string.GameView_StartGame))
        refreshUi()
    }

    /***
     * Remove the role of every player (the names stay)
     */
    private fun resetRoles() {
        GameSession.resetRoles()
        binding.startNightButton.isEnabled = true
        refreshUi()
    }

    // ──────────────────────────────────────────────────────────────
    //  Display
    // ──────────────────────────────────────────────────────────────

    /***
     * Redraw the circle and the band
     */
    private fun refreshUi() {
        val state = GameState

        //Who is looked at : the called role at night, the role tapped in the band otherwise
        val atNight = state.phase == EnumPhase.NIGHT
        val focus = if (atNight) state.activeNow else state.inspected
        val focusOrder = if (atNight) state.nightSteps.getOrNull(state.stepPos) else state.inspectedOrder

        val lights = Lights.compute(focus)

        //During the wolf turn many roles are awake : show the small picture of each one
        val badges = mutableMapOf<Seat, Int>()
        if (focusOrder != null && focusOrder == state.werewolfTurn) {
            for (character in focus) {
                val seat = state.seats.firstOrNull { it.character === character } ?: continue
                badges[seat] = RoleImages.of(character)
            }
        }

        //Players of the block of the deaths, if the switch of the main screen shows them
        val purple: Set<Seat> = if (state.showDeaths)
            state.effectBlocks.filter { it.kind == EffectBlock.Kind.DEATHS }.flatMap { it.players }.toSet()
        else emptySet()

        binding.playerCircle.update(state.seats, focus, lights.targets, lights.selectors, purple, badges)
        binding.turnBand.setEntries(BandEntries.build(this), focusOrder)

        binding.prevButton.isEnabled = atNight && state.stepPos > 0
    }

    /***
     * Text of the action : the size is reduced until it fits in its box (wolf turn : a lot of text)
     */
    private fun setStatusText(text: String?) {
        val value = text ?: ""
        GameState.statusText = value
        binding.gameStatusTextView.setTextFitting(value, binding.scrollTextView)
    }

    private fun setBackground(phase: EnumPhase) {
        binding.root.setBackgroundResource(RoleImages.background(phase))
    }

    /***
     * Update the header with turn and phase
     */
    private fun updateHeader() {
        binding.turnPhase.text = String.format(getString(R.string.GameView_Header), Labels.phase(this, GameState.phase), GameState.turn)
    }
}
