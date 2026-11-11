package main.werewolfkotlin

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import main.werewolfkotlin.databinding.ActivityGameBinding
import model.*

/***
 * //Phase of the game
 */
enum class EnumPhase {
    SLEEPING,
    NIGHT,
    DAWN,
    DAY,
    SUNSET
}

/***
 * Map on information to affect and keep. Is only used by activities deployed during the game
 */
val infoMap: MutableMap<Int, String> = mutableMapOf()

/***
 * Contains the list of active roles that are 'consumable'. Is only used by activities deployed during the game
 */
var roleListConsumable : MutableMap<CharacterGame, Boolean>  = mutableMapOf()


/**
 * List of the characters (the alive ones)
 */
var characterGames : MutableList<CharacterGame> = mutableListOf()

class GameActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityGameBinding

    //List of dead characters (marked by the moderator, confirmed at the end of the day)
    private val deadCharacterGames : MutableList<CharacterGame> = mutableListOf()

    //Game is over or not. Only used for now if activity is badly initialized
    private var gameInProgress : Boolean = true

    //Night navigation. The steps are frozen when the night starts, so going back never has to undo anything
    private var nightSteps : List<Int> = emptyList()                       //Orders called tonight
    private var nightActive : Map<Int, List<CharacterGame>> = emptyMap()   //Who plays at each step
    private var stepPos : Int = -1                                         //Position in nightSteps
    private val calledOrders : MutableSet<Int> = mutableSetOf()            //Steps already shown once tonight
    private val stepTexts : MutableMap<Int, String> = mutableMapOf()       //Text of the steps already shown

    //Characters displayed as "called" on the circle
    private var activeNow : List<CharacterGame> = emptyList()

    //Phase handler. Start in evening to be in sleeping phase when starting the game
    private var phase : EnumPhase = EnumPhase.SUNSET
        set(value) {
            field = value
            GameState.phase = value
        }

    //Turn order reference of werewolf. -1 : nobody calls the pack of werewolves
    private var werewolfTurn : Int = -1

    //Game turn tracing
    private var gameTurn : Int = 0
        set(value) {
            field = value
            GameState.turn = value
        }

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameState.reset()

        binding.gameStatusTextView.text = getString(R.string.GameView_StartGame)

        setBackground(phase)

        // Retrieve the JSON string from the Intent
        val jsonString = intent.getStringExtra("GameList")

        // Convert the JSON string back to a MutableList
        val gson = Gson()
        val type = object : TypeToken<List<CharacterGame>>() {}.type
        var chars : List<CharacterGame> = listOf()

        try {
            //Get the list from the JSON
            chars = gson.fromJson(jsonString, type)
        } catch (ex : Exception) {
            println(ex)

        }

        if(chars.isNotEmpty()) {
            deserializeProperly(chars)
        } else {//Bad catch up
            gameInProgress = false
        }

        //Set the listener for the only button
        binding.startNightButton.setOnClickListener { onMainButton() }
        binding.prevButton.setOnClickListener { goPrev() }

        updateHeader()

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            finish()
        }

        binding.InformationButton.setOnClickListener {
            startActivity(Intent(this, InformationActivity::class.java))
        }

        binding.RoleButton.setOnClickListener {
            startActivity(Intent(this, RoleActivity::class.java))
        }

        //Tap on a player : name, role, death
        binding.playerCircle.onSeatTapped = { index -> showSeatDialog(index) }

        //Turn order band
        binding.turnBand.onSelect = { order -> jumpTo(order) }
        binding.turnBand.onToggleAwake = { order -> toggleAwake(order) }
        binding.turnBand.onOpenOrder = { showTurnOrderDialog() }

        refreshUi()
    }

    /***
     * Back from the other screens : roles, infected players... may have changed
     */
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refreshUi()
    }

    /***
     * Override of destroying
     */
    override fun onDestroy() {
        super.onDestroy()
        infoMap.clear()
        characterGames.clear()
        roleListConsumable.clear()
        seats.clear()
        GameState.reset()
    }

    /***
     * Main button : next role at night, next phase otherwise
     */
    private fun onMainButton() {
        if (gameInProgress) {
            if(phase == EnumPhase.NIGHT)
                goNext()
            else
                nextPhase()
        } else {
            //Only on bad catch up
            binding.gameStatusTextView.text = getString(R.string.GameView_BadInit)
        }
    }


    /***
     * Create the good list of characters
     * GSon doesn't handle polymorphism, so we recreate each object
     */
    private fun deserializeProperly(chars : List<CharacterGame>) {

        //The marker of the werewolf turn is not a player
        val wolfTurnMarker : CharacterGame? = chars.firstOrNull { it.className == WOLF_TURN }

        chars.filter { it.className != WOLF_TURN }.forEach { character ->

            val characterRedefined =
                WorkerEasier.getGoodCharacterCast(character, character.className)

            characterGames.add(characterRedefined)

        }
        //Order the list again just in case
        characterGames.sortBy { x -> x.order }

        //Keep every role, the dead ones are removed from characterGames
        GameState.allCharacters.addAll(characterGames)

        characterGames.filter { x -> x.powerState == PowerState.CONSUMABLE }.forEach { character ->
            roleListConsumable[character] = false
        }

        //One empty seat per player. The roles are given by the moderator on the circle
        seats.clear()
        for (i in characterGames.indices) {
            seats.add(Seat(getString(R.string.SeatDefaultName, i + 1)))
        }

        //Blocks of the "Roles" tab : one per role that can have targets
        characterGames
            .distinctBy { kindOf(it) }
            .filter { it.className != "Villager" && it.className != "VillagerVillager" && it.className != "Werewolf" }
            .sortedBy { it.order }
            .forEach { character ->
                if (character is WolfFather)
                    GameState.effectBlocks.add(EffectBlock(getString(R.string.Block_Infected), EffectBlock.Kind.INFECTION))
                else
                    GameState.effectBlocks.add(EffectBlock(character.name))
            }

        //Turn of the pack of werewolves : the simple werewolves bring it,
        //otherwise it only exists if the moderator placed it when choosing the roles
        val simpleWerewolf : CharacterGame? = characterGames.firstOrNull { it is Werewolf }
        werewolfTurn = when {
            simpleWerewolf != null -> simpleWerewolf.order
            wolfTurnMarker != null && wolfTurnMarker.isNocturnal -> wolfTurnMarker.order
            else -> -1
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Night navigation
    // ──────────────────────────────────────────────────────────────

    /***
     * Who plays at this order. Same rules as before the redesign,
     * plus the infected players who wake up with the pack of werewolves.
     * The result can be empty, or only made of sleeping characters : the step is then skipped.
     */
    private fun resolveActive(index: Int): List<CharacterGame> {
        var active : List<CharacterGame>

        if(index == werewolfTurn) {
            active = characterGames.filter { x -> (x.isWerewolf && x.isNocturnal) || x.infected }

            //If there is only one werewolf at this turn, better skip it
            if(active.size == 1 && (active[0] is BigBadWolf || active[0] is WhiteWerewolf))
                active = emptyList()

        } else {
            active = characterGames.filter { x -> x.order == index }

            //Special cases : if in the unity there are at the same time sleeping roles and awaken roles
            //We filter the non awaken roles
            if(active.any { it.isNocturnal } && active.any { !it.isNocturnal })
                active = active.filter { it.isNocturnal }
        }

        return active
    }

    /***
     * Somebody is awake at this step. The pack of werewolves is always awake, infected included
     */
    private fun isStepAwake(index: Int, active: List<CharacterGame>): Boolean {
        return active.isNotEmpty() && (index == werewolfTurn || active.all { it.isNocturnal })
    }

    /***
     * Freeze tonight's steps : only the ones where somebody is awake
     */
    private fun freezeNight() {
        val maxKey : Int = characterGames.maxOfOrNull { x -> x.order } ?: -1

        val steps = mutableListOf<Int>()
        val actives = mutableMapOf<Int, List<CharacterGame>>()
        for (index in 0..maxKey) {
            val active = resolveActive(index)
            if (isStepAwake(index, active)) {
                steps.add(index)
                actives[index] = active
            }
        }

        nightSteps = steps
        nightActive = actives
        stepPos = -1
        calledOrders.clear()
        stepTexts.clear()
        activeNow = emptyList()

        GameState.pendingNocturnal.clear()
        GameState.nightSnapshot.clear()
        characterGames.forEach { GameState.nightSnapshot[it] = it.isNocturnal }
    }

    /***
     * Next role of the night. After the last one, the night ends
     */
    private fun goNext() {
        if (stepPos + 1 < nightSteps.size) {
            stepPos++
            showStep(nightSteps[stepPos])
        } else {
            nextPhase()
        }
    }

    /***
     * Previous role of the night
     */
    private fun goPrev() {
        if (phase == EnumPhase.NIGHT && stepPos > 0) {
            stepPos--
            showStep(nightSteps[stepPos])
        }
    }

    /***
     * Call (or call again) a precise role from the band
     */
    private fun jumpTo(order: Int) {
        if (phase != EnumPhase.NIGHT) {
            Toast.makeText(this, R.string.Band_CallOnlyAtNight, Toast.LENGTH_SHORT).show()
            return
        }
        val position = nightSteps.indexOf(order)
        if (position < 0) {
            Toast.makeText(this, R.string.Band_NotCalledTonight, Toast.LENGTH_SHORT).show()
            return
        }
        stepPos = position
        showStep(order)
    }

    /***
     * Show a step of the night. The effects of a role (unique power, wild child) only happen the first time
     */
    private fun showStep(order: Int) {
        val actives = nightActive[order].orEmpty()

        if (calledOrders.add(order)) {
            if(actives.size == 1 && actives[0].className == "WildChild") {
                setupWildChild(actives[0])
            }
            stepTexts[order] = describe(order, actives)
        }

        binding.gameStatusTextView.text = stepTexts[order]
        activeNow = actives
        refreshUi()
    }

    /***
     * Build the text of the actions
     */
    private fun describe(order: Int, characters : List<CharacterGame>) : String {
        //Infected players only follow the pack, their own role is called at its own turn
        val shown = if (order == werewolfTurn) characters.filter { !it.infected } else characters

        //Basic string
        var description = ""

        //Append each strict with EACH class
        for(character in shown.distinctBy { x -> x.name }) {
            description += "${character.name}\n${character.action()}\n\n"
        }

        if (order == werewolfTurn) {
            val infectedNames = seats
                .filter { it.alive && it.character?.infected == true }
                .joinToString(", ") { it.name }
            if (infectedNames.isNotEmpty())
                description += getString(R.string.Pack_Infected, infectedNames) + "\n"
        }
        return description
    }

    /***
     * Awake / asleep switch of a role.
     * During the night the change waits for the end of the night, otherwise it is immediate.
     */
    private fun toggleAwake(order: Int) {
        val members = membersOf(order)
        val entry = buildEntries().firstOrNull { it.order == order }
        if (members.isEmpty() || entry == null) return

        val newState = !(entry.pendingAwake ?: entry.awake)
        GameState.setAwakeGroup(this, members, newState, entry.label)

        refreshUi()
    }

    /***
     * Swap two moments of the turn order. Used by the window of the turn order
     */
    private fun swapOrders(orderA: Int, orderB: Int): Boolean {
        if (phase == EnumPhase.NIGHT) {
            Toast.makeText(this, R.string.Band_NightOnly, Toast.LENGTH_SHORT).show()
            return false
        }

        val first = characterGames.filter { it.order == orderA }
        val second = characterGames.filter { it.order == orderB }
        first.forEach { it.order = orderB }
        second.forEach { it.order = orderA }

        //The pack of werewolves follows its order
        if (werewolfTurn == orderA) werewolfTurn = orderB
        else if (werewolfTurn == orderB) werewolfTurn = orderA

        characterGames.sortBy { it.order }
        refreshUi()
        return true
    }

    /***
     * Window of the turn order : move the roles (drag'n'drop or arrows), wake them up or put them to sleep
     */
    private fun showTurnOrderDialog() {
        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@GameActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.6).toInt()
            )
        }

        val adapter = TurnOrderAdapter(
            buildEntries().toMutableList(),
            { a, b -> swapOrders(a, b) },
            { order -> toggleAwake(order) },
            { buildEntries() }
        )
        recycler.adapter = adapter

        val touchHelper = ItemTouchHelper(adapter.dragCallback)
        touchHelper.attachToRecyclerView(recycler)
        adapter.touchHelper = touchHelper

        AlertDialog.Builder(this)
            .setTitle(R.string.Band_OrderTitle)
            .setView(recycler)
            .setPositiveButton(R.string.Generic_Okay, null)
            .show()
    }

    // ──────────────────────────────────────────────────────────────
    //  Display
    // ──────────────────────────────────────────────────────────────

    /***
     * Characters shown in the band entry of this order
     */
    private fun membersOf(order: Int): List<CharacterGame> {
        val live = if (order == werewolfTurn)
            characterGames.filter { it.order == order || (it.isWerewolf && it.isNocturnal && !it.infected) }
        else
            characterGames.filter { it.order == order }

        return live.ifEmpty { nightActive[order].orEmpty() }
    }

    private fun isAwakeNow(order: Int): Boolean {
        return isStepAwake(order, resolveActive(order))
    }

    /***
     * Entries of the band : one per moment of the turn order
     */
    private fun buildEntries(): List<BandEntry> {
        val orders = (characterGames.map { it.order } + nightActive.keys).toSortedSet()

        return orders.map { order ->
            val members = membersOf(order)
            val awake = if (phase == EnumPhase.NIGHT) order in nightSteps else isAwakeNow(order)
            val pending = members.mapNotNull { GameState.pendingNocturnal[it] }.firstOrNull()

            val label = members.groupBy { it.name }
                .map { (name, list) -> if (list.size > 1) "$name ×${list.size}" else name }
                .joinToString(" + ")

            val icon = if (members.isNotEmpty()) WorkerEasier.getCharacterImage(members.first()) else 0

            BandEntry(order, label, icon, awake, if (pending != null && pending != awake) pending else null)
        }
    }

    /***
     * Redraw the circle and the band
     */
    private fun refreshUi() {
        binding.playerCircle.update(seats, activeNow, deadCharacterGames)

        val current = if (phase == EnumPhase.NIGHT && stepPos in nightSteps.indices) nightSteps[stepPos] else null
        binding.turnBand.setEntries(buildEntries(), current)

        binding.prevButton.isEnabled = phase == EnumPhase.NIGHT && stepPos > 0
    }

    /***
     * Name, role, awake state and death of a player
     */
    private fun showSeatDialog(index: Int) {
        val seat = seats[index]
        val originalCharacter = seat.character
        val padding = (16 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val nameInput = EditText(this).apply {
            setText(seat.name)
            setSelectAllOnFocus(true)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.SeatDialog_Name)
        }
        container.addView(nameInput)

        //Roles : the ones nobody holds yet, one line per kind of role. A dead player keeps his role
        val options = mutableListOf<CharacterGame?>(null)
        val labels = mutableListOf(getString(R.string.SeatDialog_NoRole))

        if (seat.alive) {
            val takenByOthers = seats.filter { it !== seat }.mapNotNull { it.character }
            val free = characterGames.filter { c -> takenByOthers.none { it === c } }

            if (originalCharacter != null) options.add(originalCharacter)
            free.distinctBy { kindOf(it) }
                .filter { originalCharacter == null || kindOf(it) != kindOf(originalCharacter) }
                .forEach { options.add(it) }

            labels.clear()
            options.forEach { c ->
                if (c == null) {
                    labels.add(getString(R.string.SeatDialog_NoRole))
                } else {
                    val count = free.count { kindOf(it) == kindOf(c) }
                    labels.add(if (count > 1) "${c.name} ×$count" else c.name)
                }
            }
        }

        val spinner : Spinner? = if (seat.alive) Spinner(this).apply {
            adapter = ArrayAdapter(this@GameActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(if (originalCharacter != null) 1 else 0)
        } else null
        if (spinner != null) container.addView(spinner)

        //Wake up / put to sleep the role of this player
        val awakeSwitch : SwitchCompat? = if (seat.alive && originalCharacter != null) SwitchCompat(this).apply {
            text = getString(R.string.Band_Awake)
            isChecked = GameState.shownAwake(originalCharacter)
            setPadding(0, padding / 2, 0, 0)
        } else null
        if (awakeSwitch != null) container.addView(awakeSwitch)

        fun commit() {
            seat.name = nameInput.text.toString().trim().ifEmpty { getString(R.string.SeatDefaultName, index + 1) }
            if (spinner != null) seat.character = options[spinner.selectedItemPosition]

            val character = seat.character
            if (awakeSwitch != null && character != null && character === originalCharacter
                && awakeSwitch.isChecked != GameState.shownAwake(character)) {
                GameState.setAwakeGroup(this, listOf(character), awakeSwitch.isChecked, character.name)
            }
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.SeatDialog_Title)
            .setView(container)
            .setPositiveButton(R.string.Generic_Okay) { _, _ ->
                commit()
                refreshUi()
            }
            .setNegativeButton(R.string.Generic_Cancel, null)

        if (!seat.alive) {
            //A dead player can always come back
            builder.setNeutralButton(R.string.SeatDialog_Resurrect) { _, _ ->
                seat.name = nameInput.text.toString().trim().ifEmpty { getString(R.string.SeatDefaultName, index + 1) }
                resurrect(seat)
            }
        } else if (seat.character != null && (phase == EnumPhase.DAWN || phase == EnumPhase.DAY)) {
            //During the morning and the day, the moderator also marks the deaths here
            val marked = deadCharacterGames.contains(seat.character)
            builder.setNeutralButton(if (marked) R.string.SeatDialog_UnmarkDead else R.string.SeatDialog_MarkDead) { _, _ ->
                commit()
                seat.character?.let { character ->
                    if (deadCharacterGames.remove(character)) {
                        GameState.log(this, R.string.History_Alive, seat.name, character.name)
                    } else {
                        deadCharacterGames.add(character)
                        GameState.log(this, R.string.History_Dead, seat.name, character.name)
                    }
                }
                refreshUi()
            }
        }

        builder.show()
    }

    /***
     * Bring a dead player back to the game
     */
    private fun resurrect(seat: Seat) {
        val character = seat.character ?: return

        seat.alive = true
        if (characterGames.none { it === character }) {
            characterGames.add(character)
            characterGames.sortBy { it.order }
        }

        //The game may have been over
        if (!gameInProgress) {
            gameInProgress = true
            binding.startNightButton.isEnabled = true
            binding.gameStatusTextView.text = ""
        }

        GameState.log(this, R.string.History_Alive, seat.name, character.name)
        refreshUi()
    }

    /***
     * Set the background image
     */
    private fun setBackground(phase: EnumPhase) {
        binding.root.setBackgroundResource(WorkerEasier.getBackgroundImage(phase))
    }

    /***
     * Update the header with turn and phase
     */
    private fun updateHeader() {
        binding.turnPhase.text = String.format(getString(R.string.GameView_Header), WorkerEasier.mapPhaseTranslation[phase], gameTurn)
    }

    /***
     * Remove the dead from the active list
     */
    private fun removeDeadCharacters() {
        if(deadCharacterGames.size > 0) {
            //The players keep their place on the circle, but are shown as dead
            seats.filter { seat -> seat.character != null && deadCharacterGames.contains(seat.character) }
                .forEach { it.alive = false }

            for (characterDead in deadCharacterGames) {
                characterGames.remove(characterDead)

                //Update specific consumable list
                if(roleListConsumable.contains(characterDead))
                    roleListConsumable.remove(characterDead)
            }

            //Special updates
            specialUpdatesAfterDeath();

            //Always clear the dead list
            deadCharacterGames.clear()
        }

        //If the user empty the list, game over
        if(characterGames.size == 0) {
            gameInProgress = false
            binding.gameStatusTextView.text = getString(R.string.GameView_GameOver)
            binding.startNightButton.isEnabled = false
        }

    }

    /***
     * Activate the special conditions
     */
    private fun specialUpdatesAfterDeath() {
        characterGames.filter { it.powerState == PowerState.CONDITIONAL }.forEach { x ->
            when(x.condition) {
                ConditionalActivation.LINKEDROLES -> {
                    if(!characterGames.any { x.rolesToStick.contains(it.className.uppercase().plus("-").plus(it.mode.uppercase())) }) {
                        changeNocturnePower(x)
                    }
                }

                ConditionalActivation.ONEWEREWOLF -> {
                    if(deadCharacterGames.any { it.isWerewolf }) {

                        //Unique special case for big bad wolf
                        if(x is BigBadWolf && x.mode == "NORMAL" && werewolfTurn >= 0) {
                            x.order = werewolfTurn
                            x.powerState = PowerState.PERMANENT
                        } else {
                            changeNocturnePower(x)
                        }


                    }
                }
                ConditionalActivation.ONEVILLAGER -> {
                    if(deadCharacterGames.any { !it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.ALLWEREWOLVES -> {
                    if(!characterGames.any { it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.ALLVILLAGERS -> {
                    if(characterGames.filter { it != x }.all { it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.ISALONE -> {
                    if(characterGames.filter { it.className == x.className }.size == 1) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.NOCONDITION -> {}
            }
        }

    }

    private fun changeNocturnePower(characterGame: CharacterGame) {
        characterGame.isNocturnal = characterGame.originalNightState!! //Always the reverse
        characterGame.powerState = PowerState.PERMANENT //Become permanent
    }

    /***
     * Setup the wild child for the turn of werewolf but not awaken
     */
    private fun setupWildChild(child: CharacterGame) {
        if (werewolfTurn >= 0) child.order = werewolfTurn
        child.isWerewolf = true
        child.isNocturnal = false
    }

    /***
     * Global function handling all phases
     */
    private fun nextPhase() {
        when(phase) {
            EnumPhase.SLEEPING -> nightPhase()
            EnumPhase.NIGHT -> wakingPhase()
            EnumPhase.DAWN -> dayPhase()
            EnumPhase.DAY -> eveningPhase()
            EnumPhase.SUNSET -> sleepingPhase()
        }
    }

    /***
     * Actions on sleeping Phase
     */
    private fun sleepingPhase() {
        phase = EnumPhase.SLEEPING //Set the phase
        setBackground(phase)
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseSleeping) // Change text
        stepPos = -1 // Reset index for the next night
        activeNow = emptyList()
        gameTurn++ //Starting new turn
        updateHeader()
        refreshUi()
    }

    /***
     * Actions on night Phase
     */
    private fun nightPhase() {
        phase = EnumPhase.NIGHT // Night phase
        setBackground(phase)
        updateHeader()
        freezeNight() // The steps of tonight don't change anymore
        goNext() // Start showing night roles at the first moment
    }

    /***
     * Actions on waking Phase
     */
    private fun wakingPhase() {
        GameState.applyPending() // Changes of status asked during the night are now effective
        phase = EnumPhase.DAWN // Waking phase
        setBackground(phase)
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseDawn) // Morning message
        updateHeader()
        activeNow = emptyList()
        refreshUi()
    }

    /***
     * Actions on day Phase
     */
    private fun dayPhase() {
        phase = EnumPhase.DAY // Day phase
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseDay)// Day message
        setBackground(phase)
        removeDeadCharacters() // Remove characters dead by night
        updateHeader()
        refreshUi()
    }

    /***
     * Actions on evening Phase
     */
    private fun eveningPhase() {
        phase = EnumPhase.SUNSET // Evening phase
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseEvening) // Evening message
        setBackground(phase)
        removeDeadCharacters() // Remove the dead
        updateHeader()
        refreshUi()
    }

}
