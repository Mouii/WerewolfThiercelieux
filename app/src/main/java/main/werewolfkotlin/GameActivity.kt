package main.werewolfkotlin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Layout
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.content.res.ColorStateList
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
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

    //The game is over for good (end button) : nothing to resume
    private var discardSave : Boolean = false

    //Purple marking of the players of the block of the deaths (switch of the main screen)
    private var showDeaths : Boolean = true

    //Role looked at from the band outside of the night : its holders and its lights are shown
    private var inspectedOrder : Int? = null
    private var inspected : List<CharacterGame> = emptyList()

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

        setStatusText(getString(R.string.GameView_StartGame))

        setBackground(phase)

        //Players chosen in the game setup (an empty name is the default "Player N")
        val names : List<String> = intent.getStringArrayListExtra("PlayerNames") ?: arrayListOf()

        //Back to a game that was left : the setup page offers to resume it
        val resume = intent.getBooleanExtra("Resume", false) || (savedInstanceState != null && names.isEmpty())
        val saved = if (resume) GameSave.read(this) else null

        if(saved != null && restoreGame(saved)) {
            binding.deathsSwitch.isChecked = showDeaths
        } else if(names.isNotEmpty()) {
            setupGame(names)
        } else {//Bad catch up
            gameInProgress = false
        }

        //Set the listener for the only button
        binding.startNightButton.setOnClickListener { onMainButton() }
        binding.prevButton.setOnClickListener { goPrev() }
        binding.randomButton.setOnClickListener { confirmRandomRoles() }
        binding.resetRolesButton.setOnClickListener { confirmResetRoles() }
        binding.restartButton.setOnClickListener { confirmRestart() }
        binding.deathsSwitch.setOnCheckedChangeListener { _, checked ->
            showDeaths = checked
            refreshUi()
        }

        updateHeader()

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            //The game is over : nothing to resume
            discardSave = true
            GameSave.clear(this)
            finish()
        }

        binding.InformationButton.setOnClickListener {
            startActivity(Intent(this, InformationActivity::class.java))
        }

        binding.RoleButton.setOnClickListener {
            startActivity(Intent(this, RoleActivity::class.java))
        }

        //Tap on a player : name, role, death
        binding.playerCircle.onSeatTapped = { index -> onSeatTap(index) }
        binding.playerCircle.onSeatLongPressed = { index -> showSeatDialog(index) }

        //Turn order band
        binding.turnBand.onSelect = { order -> jumpTo(order) }
        binding.turnBand.onDescribe = { order -> showRoleDescription(order) }
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
            setStatusText(getString(R.string.GameView_BadInit))
        }
    }


    /***
     * Prepare the game : one seat per player, and the roles that can be given
     */
    private fun setupGame(names : List<String>) {
        RoleOrder.load(this)

        names.forEachIndexed { i, name ->
            seats.add(Seat(name.ifBlank { getString(R.string.SeatDefaultName, i + 1) }))
        }

        //Roles of the activated extensions. They join the game when they are given to a player
        GameState.catalog = Extensions.availableRoles(this)
        GameState.syncBlocks(this)
        updateWerewolfTurn()
    }

    /***
     * Turn of the pack of werewolves : the simple werewolves bring it, otherwise it only exists
     * if the moderator asked for it in the turn order window (or if somebody is infected)
     */
    private fun updateWerewolfTurn() {
        val simpleWerewolf : CharacterGame? = characterGames.firstOrNull { it is Werewolf }
        werewolfTurn = when {
            simpleWerewolf != null -> simpleWerewolf.order
            GameState.wolfTurnForced || characterGames.any { it.isInfected } -> RoleOrder.orderOf("Werewolf")
            else -> -1
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Roles : added to the game when they are given to a player
    // ──────────────────────────────────────────────────────────────

    /***
     * Number of players having a role of this class (dead players included)
     */
    private fun countOfClass(className : String) : Int = GameState.allCharacters.count { it.className == className }

    /***
     * Moment of the night of a new role : the one of its class, the one of the roles it is linked to
     * (the wolves play with the werewolves), otherwise its usual place
     */
    private fun orderFor(prototype : CharacterGame) : Int {
        val sameClass = characterGames.firstOrNull { it.className == prototype.className }
        if(sameClass != null) return sameClass.order

        if(prototype.rolesToStick.isNotEmpty()) {
            val linked = characterGames.firstOrNull { x ->
                prototype.rolesToStick.contains(x.className.plus("-").plus(x.mode).uppercase())
            }
            if(linked != null) return linked.order
        }
        return RoleOrder.orderOf(prototype.className)
    }

    /***
     * Give a role to a player, or take it away (null). The role is added, or removed,
     * everywhere : turn order, information, roles screen.
     */
    private fun setRole(seat : Seat, prototype : CharacterGame?) {
        val old = seat.character
        if(old != null && prototype != null && kindOf(old) == kindOf(prototype)) return

        if(old != null) {
            characterGames.remove(old)
            GameState.allCharacters.remove(old)
            roleListConsumable.remove(old)
            GameState.pendingNocturnal.remove(old)
            GameState.nightSnapshot.remove(old)
            old.isInfected = false
        }
        seat.character = null

        if(prototype != null) {
            val created = prototype.clone()
            created.doTargeting = prototype.doTargeting
            created.doKilling = prototype.doKilling
            created.order = orderFor(prototype)

            characterGames.add(created)
            characterGames.sortBy { it.order }
            GameState.allCharacters.add(created)
            if(created.powerState == PowerState.CONSUMABLE) roleListConsumable[created] = false

            seat.character = created
        }

        GameState.syncBlocks(this)
        updateWerewolfTurn()
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
            active = characterGames.filter { x -> (x.isWerewolf && x.isNocturnal) || x.isInfected }

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
            inspect(order)
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

        setStatusText(stepTexts[order])
        activeNow = actives
        refreshUi()
    }

    /***
     * Build the text of the actions
     */
    private fun describe(order: Int, characters : List<CharacterGame>) : String {
        //Infected players only follow the pack, their own role is called at its own turn
        val shown = if (order == werewolfTurn) characters.filter { !it.isInfected } else characters

        //Basic string
        var description = ""

        //Append each strict with EACH class
        for(character in shown.distinctBy { x -> x.name }) {
            description += "${character.name}\n${character.action()}\n\n"
        }

        if (order == werewolfTurn) {
            val infectedNames = seats
                .filter { it.alive && it.character?.isInfected == true }
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
        val first = characterGames.filter { it.order == orderA }
        val second = characterGames.filter { it.order == orderB }
        first.forEach { it.order = orderB }
        second.forEach { it.order = orderA }

        //The pack of werewolves follows its order
        if (werewolfTurn == orderA) werewolfTurn = orderB
        else if (werewolfTurn == orderB) werewolfTurn = orderA

        //The new places are kept for the next games
        first.forEach { RoleOrder.set(this, it.className, it.order) }
        second.forEach { RoleOrder.set(this, it.className, it.order) }
        if (characterGames.none { it is Werewolf } && werewolfTurn >= 0) RoleOrder.set(this, "Werewolf", werewolfTurn)

        //During the night, the steps of the night follow : the role that is called stays the called one
        if (phase == EnumPhase.NIGHT) {
            fun moved(order: Int) = when (order) {
                orderA -> orderB
                orderB -> orderA
                else -> order
            }

            val current = if (stepPos in nightSteps.indices) nightSteps[stepPos] else null
            nightSteps = nightSteps.map { moved(it) }.sorted()
            nightActive = nightActive.mapKeys { moved(it.key) }

            val called = calledOrders.map { moved(it) }
            calledOrders.clear()
            calledOrders.addAll(called)

            val texts = stepTexts.mapKeys { moved(it.key) }
            stepTexts.clear()
            stepTexts.putAll(texts)

            if (current != null) stepPos = nightSteps.indexOf(moved(current))
        }

        characterGames.sortBy { it.order }
        refreshUi()
        return true
    }

    /***
     * Window of the turn order : move the roles (drag'n'drop or arrows), wake them up or put them to sleep
     */
    private fun showTurnOrderDialog() {
        val panel = PanelDialog(this, getString(R.string.Band_OrderTitle), scrollable = false)

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@GameActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.5).toInt()
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

        //Without simple werewolf, the moderator decides if the pack of werewolves is called
        if (characterGames.none { it is Werewolf }) {
            val wolfSwitch = SwitchCompat(this).apply {
                text = getString(R.string.WolfTurn_Switch)
                setTextColor(Color.WHITE)
                isChecked = GameState.wolfTurnForced
                setOnCheckedChangeListener { _, checked ->
                    GameState.wolfTurnForced = checked
                    updateWerewolfTurn()
                    refreshUi()
                    adapter.refresh()
                }
            }
            panel.body.addView(wolfSwitch, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() })
        }

        panel.body.addView(recycler)
        panel.addButton(getString(R.string.Generic_Okay), accent = true)
        panel.show()
    }

    /***
     * Look at a role outside of the night : who has it, and its red and green lights. Tap again to stop
     */
    private fun inspect(order: Int) {
        if (inspectedOrder == order) {
            inspectedOrder = null
            inspected = emptyList()
        } else {
            inspectedOrder = order
            inspected = membersOf(order)
        }
        refreshUi()
    }

    /***
     * Picture and description (not the action) of the roles of a moment of the turn order
     */
    private fun showRoleDescription(order: Int) {
        val members = membersOf(order).distinctBy { kindOf(it) }
        if (members.isEmpty()) return

        val panel = PanelDialog(this, members.joinToString(" + ") { it.name })
        val density = resources.displayMetrics.density

        for (character in members) {
            val picture = ImageView(this).apply {
                val res = WorkerEasier.getCharacterImage(character)
                if (res != 0) setImageResource(res)
                adjustViewBounds = true
            }
            panel.body.addView(picture, LinearLayout.LayoutParams((200 * density).toInt(), (200 * density).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            })
            panel.body.addView(panel.text(character.name, 17f, PanelDialog.CYAN, true))
            panel.body.addView(panel.text(character.description, 15f))
        }

        panel.addButton(getString(R.string.Generic_Okay), accent = true)
        panel.show()
    }

    /***
     * Lights of the called roles : red for the players they chose, green for the players who chose them.
     * Only the roles whose option is enabled in the information window count.
     */
    private fun computeLights(focus: List<CharacterGame>): Pair<Set<Seat>, Set<Seat>> {
        val targets = mutableSetOf<Seat>()
        val selectors = mutableSetOf<Seat>()

        for (character in focus) {
            val key = kindOf(character)
            val seatOfRole = seats.firstOrNull { it.character === character }

            for (block in GameState.effectBlocks) {
                val owner = block.roleKey
                if (!block.lights || owner == null) continue

                //During the turn of the role : the players it chose
                if (owner == key) targets.addAll(block.players)

                //During the turn of a chosen player : the players of the role who chose it
                if (seatOfRole != null && block.players.any { it === seatOfRole }) {
                    seats.filter { s -> s.character != null && kindOf(s.character!!) == owner }.forEach { selectors.add(it) }
                }
            }
        }
        return Pair(targets, selectors)
    }

    /***
     * Random roles, in steps :
     * 1. the moderator chooses the roles to give (and the variant of each one)
     * 2. a question reminds what will happen
     * 3. if players who already have a role have to be replaced, the moderator chooses them
     */
    private fun confirmRandomRoles() {
        if (seats.none { it.alive } || GameState.catalog.isEmpty()) {
            Toast.makeText(this, R.string.RandomRoles_None, Toast.LENGTH_SHORT).show()
            return
        }
        showRandomSelection()
    }

    private fun roleless() : List<Seat> = seats.filter { it.alive && it.character == null }

    /***
     * Step 1 : the roles to give. Tap a card to add one (a window asks the variant if there are some),
     * long press to remove one. At the limit of the role, a tap removes them all.
     */
    private fun showRandomSelection() {
        val density = resources.displayMetrics.density
        val panel = PanelDialog(this, getString(R.string.RandomRoles_Title))
        val chosen = mutableListOf<CharacterGame>()
        val groups = GameState.catalog.groupBy { it.className }.values.sortedBy { it.first().name }

        panel.body.addView(panel.text(getString(R.string.RandomPick_Help), 12f, Color.parseColor("#8FA3B8")))
        val gridHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel.body.addView(gridHolder)

        //Copies of a role that can still be added (the roles already given count)
        fun remaining(variants : List<CharacterGame>) : Int {
            val className = variants.first().className
            return variants.first().maxOccurrence - countOfClass(className) - chosen.count { it.className == className }
        }

        panel.addButton(getString(R.string.Generic_Cancel))
        val validate = panel.addButton(getString(R.string.Generic_Okay), accent = true, closes = false) {
            showRandomConfirm(chosen.toList(), panel)
        }

        fun render() {
            gridHolder.removeAllViews()

            val tiles = groups.map { variants ->
                val first = variants.first()
                val count = chosen.count { it.className == first.className }

                val tile = panel.cardTile(WorkerEasier.getCharacterImage(first), first.name, count > 0) {
                    if (remaining(variants) > 0) {
                        if (variants.size > 1) {
                            showVariantDialog(variants, 0) { variant -> chosen.add(variant); render() }
                        } else {
                            chosen.add(first)
                            render()
                        }
                    } else if (count > 0) {
                        chosen.removeAll { it.className == first.className }
                        render()
                    }
                }
                tile.setOnLongClickListener {
                    val last = chosen.lastOrNull { it.className == first.className }
                    if (last != null) {
                        chosen.remove(last)
                        render()
                    }
                    true
                }
                if (count == 0 && remaining(variants) <= 0) tile.alpha = 0.35f

                //Number of copies chosen, on the card
                val frame = FrameLayout(this)
                frame.addView(tile, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
                if (count > 0) {
                    frame.addView(TextView(this).apply {
                        text = "×$count"
                        textSize = 13f
                        setTextColor(Color.BLACK)
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setBackgroundColor(PanelDialog.CYAN)
                        setPadding((5 * density).toInt(), 0, (5 * density).toInt(), 0)
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.TOP or android.view.Gravity.END))
                }
                frame
            }
            gridHolder.addView(panel.cardGrid(tiles))

            //Hint between the roles and the buttons
            val selected = chosen.size
            val players = seats.count { it.alive }
            val withoutRole = roleless().size
            when {
                selected > players ->
                    panel.setHint(getString(R.string.RandomPick_TooMany, players), Color.parseColor("#FF6B6B"))
                selected > withoutRole ->
                    panel.setHint(getString(R.string.RandomPick_Replace, selected - withoutRole), Color.parseColor("#FFB300"))
                else ->
                    panel.setHint(getString(R.string.RandomPick_Selected, selected, withoutRole))
            }

            val valid = selected in 1..players
            validate.isEnabled = valid
            validate.alpha = if (valid) 1f else 0.4f
        }

        render()
        panel.show()
    }

    /***
     * Step 2 : the question. It reminds if there are less roles than players without role,
     * or if players who already have a role will have to be replaced.
     */
    private fun showRandomConfirm(chosen : List<CharacterGame>, selection : PanelDialog) {
        val selected = chosen.size
        val withoutRole = roleless().size

        val panel = PanelDialog(this, getString(R.string.RandomRoles_Title))
        panel.body.addView(panel.text(getString(R.string.RandomConfirm_Text, selected)))
        if (selected < withoutRole) {
            panel.body.addView(panel.text(
                getString(R.string.RandomConfirm_Fewer, selected, withoutRole, withoutRole - selected),
                14f, Color.parseColor("#FFB300")))
        }
        if (selected > withoutRole) {
            panel.body.addView(panel.text(
                getString(R.string.RandomConfirm_Replace, selected - withoutRole),
                14f, Color.parseColor("#FFB300")))
        }

        panel.addButton(getString(R.string.Generic_Cancel))
        panel.addButton(getString(R.string.Generic_Yes), accent = true) {
            if (selected > withoutRole) {
                showReplacePicker(chosen, selected - withoutRole, selection)
            } else {
                applyRandomRoles(chosen, emptyList())
                selection.dismiss()
            }
        }
        panel.show()
    }

    /***
     * Step 3 : the players who already have a role, the moderator checks the ones that are randomized.
     * As many as needed, so every chosen role finds a player.
     */
    private fun showReplacePicker(chosen : List<CharacterGame>, needed : Int, selection : PanelDialog) {
        val density = resources.displayMetrics.density
        val candidates = seats.filter { it.alive && it.character != null }

        val panel = PanelDialog(this, getString(R.string.RandomReplace_Title))
        val help = panel.text("", 13f, Color.parseColor("#FFB300"))
        panel.body.addView(help)

        val boxes = mutableListOf<Pair<CheckBox, Seat>>()
        lateinit var validate : TextView

        fun update() {
            val checked = boxes.count { it.first.isChecked }
            help.text = getString(R.string.RandomReplace_Help, needed, checked)
            val valid = checked == needed
            validate.isEnabled = valid
            validate.alpha = if (valid) 1f else 0.4f
        }

        for (seat in candidates) {
            val character = seat.character ?: continue
            val box = CheckBox(this).apply {
                buttonTintList = ColorStateList.valueOf(PanelDialog.CYAN)
                setOnCheckedChangeListener { _, _ -> update() }
            }
            boxes.add(Pair(box, seat))

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
                isClickable = true
                setOnClickListener { box.isChecked = !box.isChecked }
            }
            row.addView(box)
            val picture = WorkerEasier.getCharacterImage(character)
            if (picture != 0) {
                row.addView(ImageView(this).apply { setImageResource(picture) },
                    LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()))
            }
            row.addView(TextView(this).apply {
                text = seat.name + " — " + character.name
                textSize = 15f
                setTextColor(Color.WHITE)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = (8 * density).toInt() })
            panel.body.addView(row)
        }

        panel.addButton(getString(R.string.Generic_Cancel))
        validate = panel.addButton(getString(R.string.Generic_Okay), accent = true) {
            applyRandomRoles(chosen, boxes.filter { it.first.isChecked }.map { it.second })
            selection.dismiss()
        }
        update()
        panel.show()
    }

    /***
     * The chosen roles are given at random to the players without role (and to the replaced ones).
     * If there are less roles than players, the players left have no role.
     */
    private fun applyRandomRoles(chosen : List<CharacterGame>, replaced : List<Seat>) {
        replaced.forEach { setRole(it, null) }

        val players = roleless().shuffled()
        val roles = chosen.shuffled()
        for (i in 0 until minOf(players.size, roles.size)) {
            setRole(players[i], roles[i])
        }
        refreshUi()
    }

    // ──────────────────────────────────────────────────────────────
    //  Display
    // ──────────────────────────────────────────────────────────────

    /***
     * Characters shown in the band entry of this order
     */
    private fun membersOf(order: Int): List<CharacterGame> {
        val live = if (order == werewolfTurn)
            characterGames.filter { it.order == order || (it.isWerewolf && it.isNocturnal && !it.isInfected) }
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
        val orders = (characterGames.map { it.order } + nightActive.keys +
                (if (werewolfTurn >= 0) listOf(werewolfTurn) else emptyList())).toSortedSet()

        return orders.map { order ->
            val members = membersOf(order)
            val awake = if (phase == EnumPhase.NIGHT) order in nightSteps else isAwakeNow(order)
            val pending = members.mapNotNull { GameState.pendingNocturnal[it] }.firstOrNull()

            val label = members.groupBy { it.name }
                .map { (name, list) -> if (list.size > 1) "$name (${list.size})" else name }
                .joinToString(" + ")
                .ifEmpty { if (order == werewolfTurn) getString(R.string.WolfTurn_Name) else "" }

            val icon = if (members.isNotEmpty()) WorkerEasier.getCharacterImage(members.first())
                       else if (order == werewolfTurn) R.drawable.werewolf else 0

            BandEntry(order, label, icon, awake, if (pending != null && pending != awake) pending else null)
        }
    }

    /***
     * Redraw the circle and the band
     */
    private fun refreshUi() {
        //Who is looked at : the called role at night, the role tapped in the band otherwise
        val atNight = phase == EnumPhase.NIGHT
        val focus = if (atNight) activeNow else inspected
        val focusOrder = if (atNight) {
            if (stepPos in nightSteps.indices) nightSteps[stepPos] else null
        } else {
            inspectedOrder
        }

        val (targets, selectors) = computeLights(focus)

        //During the wolf turn many roles are awake : show the small picture of each one
        val badges = mutableMapOf<Seat, Int>()
        if (focusOrder != null && focusOrder == werewolfTurn) {
            for (character in focus) {
                val seat = seats.firstOrNull { it.character === character } ?: continue
                badges[seat] = WorkerEasier.getCharacterImage(character)
            }
        }

        //Players of the block of the deaths, if the switch of the main screen shows them
        val purple : Set<Seat> = if (showDeaths)
            GameState.effectBlocks.filter { it.kind == EffectBlock.Kind.DEATHS }.flatMap { it.players }.toSet()
        else emptySet()

        binding.playerCircle.update(seats, focus, targets, selectors, purple, badges)
        binding.turnBand.setEntries(buildEntries(), focusOrder)

        binding.prevButton.isEnabled = phase == EnumPhase.NIGHT && stepPos > 0
    }

    /***
     * Text of the action : the size is reduced until it fits in its box (wolf turn : a lot of text)
     */
    private fun setStatusText(text : String?) {
        val value = text ?: ""
        val view = binding.gameStatusTextView
        view.text = value

        binding.scrollTextView.post {
            val width = binding.scrollTextView.width - view.paddingLeft - view.paddingRight
            val height = binding.scrollTextView.height - view.paddingTop - view.paddingBottom
            if (width <= 0 || height <= 0 || value.isEmpty()) return@post

            var size = 16f
            while (size > 9f) {
                val paint = TextPaint(view.paint)
                paint.textSize = size * resources.displayMetrics.scaledDensity
                @Suppress("DEPRECATION")
                val layout = StaticLayout(value, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
                if (layout.height <= height) break
                size -= 1f
            }
            view.textSize = size
        }
    }

    /***
     * Tap on a player :
     * - when a role that kills is called (werewolves) : the player is selected to die, in purple (tap again to cancel)
     * - when another role is called : the player is its target, stored in the block of the role (tap again to remove)
     * - in the morning and during the day : the player is selected to die, in purple (tap again to cancel)
     * - otherwise : his window
     * A long press always opens his window.
     */
    private fun onSeatTap(index : Int) {
        val seat = seats[index]

        if (seat.alive) {
            if (phase == EnumPhase.NIGHT && (toggleKill(seat) || toggleTarget(seat))) return

            if (phase == EnumPhase.DAWN || phase == EnumPhase.DAY) {
                toggleDeath(seat)
                return
            }
        }
        showSeatDialog(index)
    }

    private fun deathsBlock() : EffectBlock? = GameState.effectBlocks.firstOrNull { it.kind == EffectBlock.Kind.DEATHS }

    /***
     * Victim of a called role that kills : it goes in the block of the deaths. False if no called role kills
     */
    private fun toggleKill(seat : Seat) : Boolean {
        val killer = activeNow.firstOrNull { it.doKilling } ?: return false
        val block = deathsBlock() ?: return false

        val message : String
        if (block.players.removeAll { it === seat }) {
            message = getString(R.string.History_DeathTargetRemoved, killer.name, seat.name)
            GameState.log(this, R.string.History_DeathTargetRemoved, killer.name, seat.name)
        } else {
            block.players.add(seat)
            message = getString(R.string.History_DeathTarget, killer.name, seat.name)
            GameState.log(this, R.string.History_DeathTarget, killer.name, seat.name)
        }

        //Purple has to be visible to be useful
        if (!binding.deathsSwitch.isChecked) binding.deathsSwitch.isChecked = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        refreshUi()
        return true
    }

    /***
     * Target of the called role. False if no called role has a block of targets
     */
    private fun toggleTarget(seat : Seat) : Boolean {
        val block = activeNow
            .firstNotNullOfOrNull { c -> GameState.effectBlocks.firstOrNull { it.kind == EffectBlock.Kind.TARGETS && it.roleKey == kindOf(c) } }
            ?: return false

        val message : String
        if (block.players.any { it === seat }) {
            block.players.removeAll { it === seat }
            message = getString(R.string.History_TargetRemoved, block.title, seat.name)
            GameState.log(this, R.string.History_TargetRemoved, block.title, seat.name)
        } else {
            block.players.add(seat)
            message = getString(R.string.History_Target, block.title, seat.name)
            GameState.log(this, R.string.History_Target, block.title, seat.name)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        refreshUi()
        return true
    }

    /***
     * Purple : the player is in the block of the deaths, he dies when the turn changes
     */
    private fun toggleDeath(seat : Seat) {
        val block = deathsBlock() ?: return
        if (!block.players.removeAll { it === seat }) block.players.add(seat)

        //Purple has to be visible to be useful
        if (!binding.deathsSwitch.isChecked) binding.deathsSwitch.isChecked = true
        refreshUi()
    }

    /***
     * The turn changes : the purple players are eliminated
     */
    private fun applyPendingDeaths() {
        val block = deathsBlock() ?: return
        val dying = block.players.filter { it.alive }
        block.players.clear()
        dying.forEach { killSeat(it) }
    }

    /***
     * Window of the variants of a role : the key on top (arrows to change it), then the picture, then the description
     */
    private fun showVariantDialog(variants : List<CharacterGame>, start : Int, onChoose : (CharacterGame) -> Unit) {
        var index = start
        val density = resources.displayMetrics.density

        val panel = PanelDialog(this, variants.first().name)

        fun arrow(symbol : String) : TextView = panel.text(symbol, 26f, PanelDialog.CYAN, true).apply {
            gravity = android.view.Gravity.CENTER
            setPadding((18 * density).toInt(), (6 * density).toInt(), (18 * density).toInt(), (6 * density).toInt())
            isClickable = true
        }

        val left = arrow("◀")
        val right = arrow("▶")
        val keyText = panel.text("", 20f, Color.WHITE, true).apply { gravity = android.view.Gravity.CENTER }

        val selector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        selector.addView(left)
        selector.addView(keyText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        selector.addView(right)
        panel.body.addView(selector)

        val picture = ImageView(this).apply { adjustViewBounds = true }
        panel.body.addView(picture, LinearLayout.LayoutParams((200 * density).toInt(), (200 * density).toInt()).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        })
        val description = panel.text("", 15f)
        panel.body.addView(description)

        fun render() {
            val variant = variants[index]
            keyText.text = variant.mode
            val res = WorkerEasier.getCharacterImage(variant)
            if (res != 0) picture.setImageResource(res) else picture.setImageDrawable(null)
            description.text = variant.description
        }

        //Nothing to change with only one key
        val several = variants.size > 1
        left.isEnabled = several
        right.isEnabled = several
        left.alpha = if (several) 1f else 0.3f
        right.alpha = if (several) 1f else 0.3f
        left.setOnClickListener { index = (index - 1 + variants.size) % variants.size; render() }
        right.setOnClickListener { index = (index + 1) % variants.size; render() }

        render()

        panel.addButton(getString(R.string.Generic_Cancel))
        panel.addButton(getString(R.string.Variant_Choose), accent = true) { onChoose(variants[index]) }
        panel.show()
    }

    /***
     * Start the game again : same players, same roles, but nobody is dead, no target, everybody sleeps
     */
    private fun confirmRestart() {
        val panel = PanelDialog(this, getString(R.string.Restart_Title))
        panel.body.addView(panel.text(getString(R.string.Restart_Confirm)))
        panel.addButton(getString(R.string.Generic_Cancel))
        panel.addButton(getString(R.string.Generic_Yes), accent = true) { restartGame() }
        panel.show()
    }

    private fun restartGame() {
        //Each role comes back to its original state (powers not used, nobody infected...) and keeps its place in the turn order
        val renewed = mutableMapOf<CharacterGame, CharacterGame>()
        for (old in GameState.allCharacters) {
            val prototype = WorkerEasier.characterListType.firstOrNull { it.first == old.className }?.second?.get(old.mode)
            if (prototype == null) {
                old.isInfected = false
                renewed[old] = old
                continue
            }
            val created = prototype.clone()
            created.doTargeting = prototype.doTargeting
            created.doKilling = prototype.doKilling
            created.order = old.order
            renewed[old] = created
        }

        val roles = GameState.allCharacters.map { renewed[it] ?: it }
        GameState.allCharacters.clear()
        GameState.allCharacters.addAll(roles)
        characterGames.clear()
        characterGames.addAll(roles)
        characterGames.sortBy { it.order }

        //Everybody is alive again, with the same role
        seats.forEach { seat ->
            seat.alive = true
            seat.character = seat.character?.let { renewed[it] ?: it }
        }

        roleListConsumable.clear()
        roles.filter { it.powerState == PowerState.CONSUMABLE }.forEach { roleListConsumable[it] = false }

        //No target, no death, no historic
        GameState.effectBlocks.forEach { it.players.clear() }
        GameState.history.clear()
        GameState.pendingNocturnal.clear()
        GameState.nightSnapshot.clear()
        GameState.eldersPowerRemoved = false
        GameState.fatherCanInfectSolo = false

        //Nothing is called anymore, the village sleeps
        nightSteps = emptyList()
        nightActive = emptyMap()
        stepPos = -1
        calledOrders.clear()
        stepTexts.clear()
        activeNow = emptyList()
        inspectedOrder = null
        inspected = emptyList()

        gameInProgress = true
        binding.startNightButton.isEnabled = true
        phase = EnumPhase.SUNSET
        gameTurn = 0
        setBackground(phase)
        updateHeader()
        setStatusText(getString(R.string.GameView_StartGame))

        updateWerewolfTurn()
        GameState.log(this, R.string.History_Restarted)
        refreshUi()
    }

    /***
     * Remove the role of every player (the names stay)
     */
    private fun confirmResetRoles() {
        val panel = PanelDialog(this, getString(R.string.ResetRoles_Title))
        panel.body.addView(panel.text(getString(R.string.ResetRoles_Confirm)))
        panel.addButton(getString(R.string.Generic_Cancel))
        panel.addButton(getString(R.string.Generic_Yes), accent = true) { resetRoles() }
        panel.show()
    }

    private fun resetRoles() {
        seats.forEach {
            it.character = null
            it.alive = true
        }
        characterGames.clear()
        GameState.allCharacters.clear()
        roleListConsumable.clear()
        GameState.pendingNocturnal.clear()
        GameState.nightSnapshot.clear()
        GameState.eldersPowerRemoved = false
        GameState.effectBlocks.forEach { it.players.clear() }

        //Nothing is called anymore
        nightSteps = emptyList()
        nightActive = emptyMap()
        stepPos = -1
        calledOrders.clear()
        stepTexts.clear()
        activeNow = emptyList()
        inspectedOrder = null
        inspected = emptyList()

        gameInProgress = true
        binding.startNightButton.isEnabled = true

        GameState.syncBlocks(this)
        updateWerewolfTurn()
        GameState.log(this, R.string.History_RolesReset)
        refreshUi()
    }

    // ──────────────────────────────────────────────────────────────
    //  Save and resume
    // ──────────────────────────────────────────────────────────────

    private fun saveGame() {
        if (discardSave || !::binding.isInitialized || seats.isEmpty()) return
        try {
            GameSave.write(this, buildSave())
        } catch (ex : Exception) {
            println(ex)
        }
    }

    private fun idOf(character : CharacterGame) : Int = GameState.allCharacters.indexOfFirst { it === character }

    private fun buildSave() : JSONObject {
        val root = JSONObject()
        root.put("phase", phase.name)
        root.put("turn", gameTurn)
        root.put("inProgress", gameInProgress)
        root.put("showDeaths", showDeaths)
        root.put("wolfForced", GameState.wolfTurnForced)
        root.put("fatherSolo", GameState.fatherCanInfectSolo)
        root.put("elders", GameState.eldersPowerRemoved)
        root.put("status", binding.gameStatusTextView.text.toString())
        root.put("stepPos", stepPos)

        //Roles given to the players, alive or dead
        val roles = JSONArray()
        for (c in GameState.allCharacters) {
            roles.put(JSONObject()
                .put("class", c.className).put("mode", c.mode).put("order", c.order)
                .put("nocturnal", c.isNocturnal).put("power", c.powerState.name)
                .put("wolf", c.isWerewolf).put("infected", c.isInfected).put("target", c.doTargeting)
                .put("alive", characterGames.any { it === c })
                .put("consumable", roleListConsumable[c] ?: false)
                .put("hasConsumable", roleListConsumable.containsKey(c)))
        }
        root.put("roles", roles)

        val seatArray = JSONArray()
        for (seat in seats) {
            seatArray.put(JSONObject()
                .put("name", seat.name).put("alive", seat.alive).put("note", seat.note)
                .put("role", seat.character?.let { idOf(it) } ?: -1))
        }
        root.put("seats", seatArray)

        val blocks = JSONArray()
        for (block in GameState.effectBlocks) {
            val o = JSONObject().put("title", block.title).put("kind", block.kind.name).put("lights", block.lights)
            block.roleKey?.let { o.put("key", JSONArray(listOf(it.first, it.second, it.third))) }
            val players = JSONArray()
            block.players.forEach { p -> players.put(seats.indexOfFirst { it === p }) }
            o.put("players", players)
            blocks.put(o)
        }
        root.put("blocks", blocks)

        val history = JSONArray()
        for (h in GameState.history) history.put(JSONObject().put("turn", h.turn).put("phase", h.phase.name).put("text", h.text))
        root.put("history", history)

        //The night in progress
        root.put("nightSteps", JSONArray(nightSteps))
        val active = JSONObject()
        nightActive.forEach { (order, list) -> active.put(order.toString(), JSONArray(list.map { idOf(it) })) }
        root.put("nightActive", active)
        root.put("called", JSONArray(calledOrders.toList()))
        val texts = JSONObject()
        stepTexts.forEach { (order, text) -> texts.put(order.toString(), text) }
        root.put("stepTexts", texts)
        val pending = JSONObject()
        GameState.pendingNocturnal.forEach { (c, v) -> pending.put(idOf(c).toString(), v) }
        root.put("pending", pending)
        val snapshot = JSONObject()
        GameState.nightSnapshot.forEach { (c, v) -> snapshot.put(idOf(c).toString(), v) }
        root.put("snapshot", snapshot)
        root.put("activeNow", JSONArray(activeNow.map { idOf(it) }))
        return root
    }

    /***
     * Put the game back as it was. False if something is wrong (a role that does not exist anymore...)
     */
    private fun restoreGame(root : JSONObject) : Boolean {
        try {
            RoleOrder.load(this)
            GameState.reset()
            seats.clear()
            characterGames.clear()
            roleListConsumable.clear()

            //Roles : a copy of the role of the list, with what changed during the game
            val all = mutableListOf<CharacterGame>()
            val roles = root.getJSONArray("roles")
            for (i in 0 until roles.length()) {
                val o = roles.getJSONObject(i)
                val prototype = WorkerEasier.characterListType.firstOrNull { it.first == o.getString("class") }
                    ?.second?.get(o.getString("mode")) ?: return false

                val c = prototype.clone()
                c.doTargeting = o.getBoolean("target")
                c.doKilling = prototype.doKilling
                c.order = o.getInt("order")
                c.isNocturnal = o.getBoolean("nocturnal")
                c.powerState = PowerState.valueOf(o.getString("power"))
                c.isWerewolf = o.getBoolean("wolf")
                c.isInfected = o.getBoolean("infected")
                all.add(c)

                if (o.getBoolean("alive")) characterGames.add(c)
                if (o.getBoolean("hasConsumable")) roleListConsumable[c] = o.getBoolean("consumable")
            }
            characterGames.sortBy { it.order }
            GameState.allCharacters.addAll(all)

            val seatArray = root.getJSONArray("seats")
            for (i in 0 until seatArray.length()) {
                val o = seatArray.getJSONObject(i)
                val seat = Seat(o.getString("name"), null, o.getBoolean("alive"), o.getString("note"))
                val role = o.getInt("role")
                if (role >= 0) seat.character = all[role]
                seats.add(seat)
            }

            GameState.catalog = Extensions.availableRoles(this)

            //Blocks of the roles tab
            val blocks = root.getJSONArray("blocks")
            for (i in 0 until blocks.length()) {
                val o = blocks.getJSONObject(i)
                val key = if (o.has("key")) {
                    val k = o.getJSONArray("key")
                    Triple(k.getString(0), k.getString(1), k.getString(2))
                } else null
                val block = EffectBlock(o.getString("title"), EffectBlock.Kind.valueOf(o.getString("kind")), key)
                block.lights = o.getBoolean("lights")
                val players = o.getJSONArray("players")
                for (j in 0 until players.length()) {
                    val seatIndex = players.getInt(j)
                    if (seatIndex in seats.indices) block.players.add(seats[seatIndex])
                }
                GameState.effectBlocks.add(block)
            }
            GameState.syncBlocks(this)

            val history = root.getJSONArray("history")
            for (i in 0 until history.length()) {
                val o = history.getJSONObject(i)
                GameState.history.add(HistoryEntry(o.getInt("turn"), EnumPhase.valueOf(o.getString("phase")), o.getString("text")))
            }

            GameState.wolfTurnForced = root.getBoolean("wolfForced")
            GameState.fatherCanInfectSolo = root.getBoolean("fatherSolo")
            GameState.eldersPowerRemoved = root.getBoolean("elders")

            phase = EnumPhase.valueOf(root.getString("phase"))
            gameTurn = root.getInt("turn")
            gameInProgress = root.getBoolean("inProgress")
            showDeaths = root.getBoolean("showDeaths")
            stepPos = root.getInt("stepPos")

            //The night in progress
            val steps = root.getJSONArray("nightSteps")
            nightSteps = (0 until steps.length()).map { steps.getInt(it) }
            val active = root.getJSONObject("nightActive")
            nightActive = active.keys().asSequence().associate { key ->
                val ids = active.getJSONArray(key)
                key.toInt() to (0 until ids.length()).map { all[ids.getInt(it)] }
            }
            val called = root.getJSONArray("called")
            calledOrders.clear()
            for (i in 0 until called.length()) calledOrders.add(called.getInt(i))
            val texts = root.getJSONObject("stepTexts")
            stepTexts.clear()
            texts.keys().forEach { stepTexts[it.toInt()] = texts.getString(it) }
            val pending = root.getJSONObject("pending")
            pending.keys().forEach { GameState.pendingNocturnal[all[it.toInt()]] = pending.getBoolean(it) }
            val snapshot = root.getJSONObject("snapshot")
            snapshot.keys().forEach { GameState.nightSnapshot[all[it.toInt()]] = snapshot.getBoolean(it) }
            val activeIds = root.getJSONArray("activeNow")
            activeNow = (0 until activeIds.length()).map { all[activeIds.getInt(it)] }

            updateWerewolfTurn()
            setBackground(phase)
            setStatusText(root.getString("status"))
            binding.startNightButton.isEnabled = gameInProgress
            return true
        } catch (ex : Exception) {
            println(ex)
            GameState.reset()
            seats.clear()
            characterGames.clear()
            return false
        }
    }

    /***
     * Name, role, awake state and death of a player.
     * Choosing a role gives it to the player and closes the window.
     */
    private fun showSeatDialog(index: Int) {
        val seat = seats[index]
        val originalCharacter = seat.character
        var roleChanged = false
        var chosenPrototype : CharacterGame? = null

        val panel = PanelDialog(this, getString(R.string.SeatDialog_Title))

        val nameInput = panel.field(seat.name, getString(R.string.SeatDialog_Name))
        panel.body.addView(nameInput)

        if (seat.alive) {
            panel.body.addView(panel.section(getString(R.string.SeatDialog_RoleTitle)))

            //The roles are cards side by side (4 per line), hidden in a closed bar that shows the role of the player
            val tiles = mutableListOf<View>()

            tiles.add(panel.cardTile(0, getString(R.string.SeatDialog_NoRole), originalCharacter == null) {
                roleChanged = originalCharacter != null
                chosenPrototype = null
                panel.dismiss()
            })

            //Roles of the activated extensions that can still be given, one line per role.
            //The role of the player stays on top. A role with variants opens a window to choose one
            val available = GameState.catalog.filter { prototype ->
                val inGame = countOfClass(prototype.className) - (if (originalCharacter?.className == prototype.className) 1 else 0)
                inGame < prototype.maxOccurrence
            }
            val groups = available.groupBy { it.className }.values
                .sortedWith(compareBy({ it.first().className != originalCharacter?.className }, { it.first().name }))

            for (variants in groups) {
                val isCurrent = originalCharacter != null && variants.first().className == originalCharacter.className
                val first = variants.first()

                tiles.add(panel.cardTile(WorkerEasier.getCharacterImage(first), first.name, isCurrent) {
                    if (variants.size == 1) {
                        if (!isCurrent) {
                            roleChanged = true
                            chosenPrototype = first
                        }
                        panel.dismiss()
                    } else {
                        val start = variants.indexOfFirst { originalCharacter != null && kindOf(it) == kindOf(originalCharacter) }.coerceAtLeast(0)
                        showVariantDialog(variants, start) { chosen ->
                            if (originalCharacter == null || kindOf(chosen) != kindOf(originalCharacter)) {
                                roleChanged = true
                                chosenPrototype = chosen
                            }
                            panel.dismiss()
                        }
                    }
                })
            }

            //Should not happen : the role of the player is not available anymore
            if (originalCharacter != null && groups.none { it.first().className == originalCharacter.className }) {
                tiles.add(panel.cardTile(WorkerEasier.getCharacterImage(originalCharacter), originalCharacter.name, true) {
                    panel.dismiss()
                })
            }

            val roleList = panel.cardGrid(tiles)
            panel.body.addView(panel.collapsible(
                if (originalCharacter != null) WorkerEasier.getCharacterImage(originalCharacter) else 0,
                originalCharacter?.name ?: getString(R.string.SeatDialog_NoRole),
                roleList
            ))
        } else if (originalCharacter != null) {
            panel.body.addView(panel.section(getString(R.string.SeatDialog_RoleTitle)))
            panel.body.addView(panel.text(originalCharacter.name, 16f, Color.WHITE, true))
        }

        val density = resources.displayMetrics.density

        //Wake up / put to sleep the role of this player
        val awakeSwitch : SwitchCompat? = if (seat.alive && originalCharacter != null) SwitchCompat(this).apply {
            text = getString(R.string.Band_Awake)
            setTextColor(Color.WHITE)
            isChecked = GameState.shownAwake(originalCharacter)
        } else null
        if (awakeSwitch != null) {
            panel.body.addView(awakeSwitch, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (14 * density).toInt() })
        }

        //Dead or alive : applied when the window closes
        val deadSwitch = SwitchCompat(this).apply {
            text = getString(R.string.SeatDialog_Dead)
            setTextColor(Color.WHITE)
            isChecked = !seat.alive
        }
        panel.body.addView(deadSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (10 * density).toInt() })

        //Whatever the way the window closes, the name, the role and the states are kept
        panel.onDismiss = {
            seat.name = nameInput.text.toString().trim().ifEmpty { getString(R.string.SeatDefaultName, index + 1) }
            PlayerNames.save(this, seats)

            if (seat.alive && roleChanged) setRole(seat, chosenPrototype)

            val character = seat.character
            if (awakeSwitch != null && character != null && character === originalCharacter
                && awakeSwitch.isChecked != GameState.shownAwake(character)) {
                GameState.setAwakeGroup(this, listOf(character), awakeSwitch.isChecked, character.name)
            }

            if (deadSwitch.isChecked && seat.alive) killSeat(seat)
            else if (!deadSwitch.isChecked && !seat.alive) resurrect(seat)

            refreshUi()
        }

        panel.addButton(getString(R.string.SeatDialog_Close), accent = true)
        panel.show()
    }

    /***
     * Bring a dead player back to the game
     */
    private fun resurrect(seat: Seat) {
        val character = seat.character

        deathsBlock()?.players?.removeAll { it === seat }
        seat.alive = true
        if (character != null && characterGames.none { it === character }) {
            characterGames.add(character)
            characterGames.sortBy { it.order }
            if (character.powerState == PowerState.CONSUMABLE && !roleListConsumable.containsKey(character))
                roleListConsumable[character] = false
        }
        updateWerewolfTurn()

        //The game may have been over
        if (!gameInProgress) {
            gameInProgress = true
            binding.startNightButton.isEnabled = true
            setStatusText("")
        }

        GameState.log(this, R.string.History_Alive, seat.name, character?.name ?: "—")
    }

    /***
     * A player dies : the role leaves the game right away (turn order, powers that depend on it)
     */
    private fun killSeat(seat: Seat) {
        val character = seat.character
        seat.alive = false
        deathsBlock()?.players?.removeAll { it === seat }
        GameState.log(this, R.string.History_Dead, seat.name, character?.name ?: "—")

        if (character == null) return

        characterGames.remove(character)
        roleListConsumable.remove(character)
        GameState.pendingNocturnal.remove(character)

        //Special powers that depend on this death
        deadCharacterGames.clear()
        deadCharacterGames.add(character)
        specialUpdatesAfterDeath()
        deadCharacterGames.clear()

        updateWerewolfTurn()

        //Game over : nobody has a role in the game anymore
        if (characterGames.isEmpty() && seats.none { it.alive && it.character == null }) {
            gameInProgress = false
            setStatusText(getString(R.string.GameView_GameOver))
            binding.startNightButton.isEnabled = false
        }
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
     * Activate the special conditions
     */
    private fun specialUpdatesAfterDeath() {
        characterGames.filter { it.powerState == PowerState.CONDITIONAL }.forEach { x ->
            when(x.condition) {
                ConditionalActivation.LINKED_PLAYERS -> {
                    if(!characterGames.any { x.rolesToStick.contains(it.className.uppercase().plus("-").plus(it.mode.uppercase())) }) {
                        changeNocturnePower(x)
                    }
                }

                ConditionalActivation.ONE_WEREWOLF -> {
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
                ConditionalActivation.ONE_VILLAGER -> {
                    if(deadCharacterGames.any { !it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.ALL_WEREWOLVES -> {
                    if(!characterGames.any { it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.ALL_VILLAGERS -> {
                    if(characterGames.filter { it != x }.all { it.isWerewolf }) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.IS_ALONE -> {
                    if(characterGames.filter { it.className == x.className }.size == 1) {
                        changeNocturnePower(x)
                    }
                }
                ConditionalActivation.NO_CONDITION -> {}
                //Activated when the morning comes, not when somebody dies (see activateMorningPowers)
                ConditionalActivation.ON_MORNING -> {}
            }
        }

    }

    /***
     * Morning : the roles with the condition ON_MORNING activate their power
     */
    private fun activateMorningPowers() {
        characterGames
            .filter { it.powerState == PowerState.CONDITIONAL && it.condition == ConditionalActivation.ON_MORNING }
            .forEach { changeNocturnePower(it) }
    }

    private fun changeNocturnePower(characterGame: CharacterGame) {
        characterGame.isNocturnal = characterGame.originalNightState //Back to the night state it had at its creation
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
        inspectedOrder = null
        inspected = emptyList()

        //The turn changes : the purple players are eliminated
        if (phase == EnumPhase.DAWN || phase == EnumPhase.DAY) applyPendingDeaths()

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
        GameState.resetPermanentTargets() //New night : the permanent powers forget their targets
        setBackground(phase)
        setStatusText(getString(R.string.GameView_phaseSleeping)) // Change text
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
        activateMorningPowers() // The powers that wait for the morning
        phase = EnumPhase.DAWN // Waking phase
        setBackground(phase)
        setStatusText(getString(R.string.GameView_phaseDawn)) // Morning message
        updateHeader()
        activeNow = emptyList()
        refreshUi()
    }

    /***
     * Actions on day Phase
     */
    private fun dayPhase() {
        phase = EnumPhase.DAY // Day phase
        setStatusText(getString(R.string.GameView_phaseDay))// Day message
        setBackground(phase)
        updateHeader()
        refreshUi()
    }

    /***
     * Actions on evening Phase
     */
    private fun eveningPhase() {
        phase = EnumPhase.SUNSET // Evening phase
        setStatusText(getString(R.string.GameView_phaseEvening)) // Evening message
        setBackground(phase)
        updateHeader()
        refreshUi()
    }

}
