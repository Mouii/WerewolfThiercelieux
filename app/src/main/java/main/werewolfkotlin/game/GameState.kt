package main.werewolfkotlin.game

import model.CharacterGame

/***
 * Everything shared between the screens of a game. Cleared when GameActivity is destroyed.
 * It only holds the state : the rules that change it are in RoleRoster, NightFlow, PowerRules...
 */
object GameState {

    // ── The table ──────────────────────────────────────────────────

    /** The players around the table */
    val seats: MutableList<Seat> = mutableListOf()

    /** Every role given to a player, alive or dead (aliveCharacters only keeps the alive ones) */
    val allCharacters: MutableList<CharacterGame> = mutableListOf()

    /** The roles of the players who are alive, sorted by moment of the night */
    val aliveCharacters: MutableList<CharacterGame> = mutableListOf()

    /** Roles with a consumable power : has the power been used ? */
    val consumables: MutableMap<CharacterGame, Boolean> = mutableMapOf()

    /** The roles of the activated extensions : what can be given to a player */
    var catalog: List<CharacterGame> = emptyList()

    // ── Time ───────────────────────────────────────────────────────

    /** Start in evening to be in sleeping phase when starting the game */
    var phase: EnumPhase = EnumPhase.SUNSET
    var turn: Int = 0

    /** Game is over or not. Also false if the activity is badly initialized */
    var inProgress: Boolean = true

    /** Text of the action shown to the moderator. The screen writes it, the rules never read it */
    var statusText: String = ""

    /** The fixed labels the rules need, given by the screen at the start of the game */
    var labels: GameLabels = GameLabels.NONE

    // ── The night ──────────────────────────────────────────────────

    /** Turn order reference of werewolf. -1 : nobody calls the pack of werewolves */
    var werewolfTurn: Int = -1

    /** The pack of werewolves is called even without simple werewolf (set in the turn order window) */
    var wolfTurnForced: Boolean = false

    /** Night navigation. The steps are frozen when the night starts, so going back never has to undo anything */
    var nightSteps: List<Int> = emptyList()                       //Orders called tonight
    var nightActive: Map<Int, List<CharacterGame>> = emptyMap()   //Who plays at each step
    var stepPos: Int = -1                                         //Position in nightSteps
    val calledOrders: MutableSet<Int> = mutableSetOf()            //Steps already shown once tonight
    val stepCalls: MutableMap<Int, NightCall> = mutableMapOf()    //What was said at each step already shown

    /** Characters displayed as "called" on the circle */
    var activeNow: List<CharacterGame> = emptyList()

    /** Awake / asleep changes asked during the night, applied when the night ends */
    val pendingNocturnal: MutableMap<CharacterGame, Boolean> = mutableMapOf()

    /** State of every role when the night started */
    val nightSnapshot: MutableMap<CharacterGame, Boolean> = mutableMapOf()

    // ── What the moderator looks at ────────────────────────────────

    /** Purple marking of the players of the block of the deaths (switch of the main screen) */
    var showDeaths: Boolean = true

    /** Role looked at from the band outside of the night : its holders and its lights are shown */
    var inspectedOrder: Int? = null
    var inspected: List<CharacterGame> = emptyList()

    // ── Information window ─────────────────────────────────────────

    val history: MutableList<HistoryEntry> = mutableListOf()
    val effectBlocks: MutableList<EffectBlock> = mutableListOf()

    /** Option of the infect father of wolves : can he infect solo roles ? */
    var fatherCanInfectSolo: Boolean = false

    /** The elder power (remove the powers of the villagers) was used */
    var eldersPowerRemoved: Boolean = false

    fun reset() {
        seats.clear()
        allCharacters.clear()
        aliveCharacters.clear()
        consumables.clear()
        catalog = emptyList()

        phase = EnumPhase.SUNSET
        turn = 0
        inProgress = true
        statusText = ""
        //labels are not game data : the screen gives them once and they stay

        werewolfTurn = -1
        wolfTurnForced = false
        clearNight()
        pendingNocturnal.clear()
        nightSnapshot.clear()

        showDeaths = true
        inspectedOrder = null
        inspected = emptyList()

        history.clear()
        effectBlocks.clear()
        fatherCanInfectSolo = false
        eldersPowerRemoved = false
    }

    /***
     * Nothing is called anymore : the steps of the night, the role looked at
     */
    fun clearNight() {
        nightSteps = emptyList()
        nightActive = emptyMap()
        stepPos = -1
        calledOrders.clear()
        stepCalls.clear()
        activeNow = emptyList()
        inspectedOrder = null
        inspected = emptyList()
    }

    /***
     * Add a line to the historic, with the turn and the phase of now.
     * [names] are the players or roles the message speaks about, in the order of the message.
     */
    fun log(message: GameMessage, vararg names: String) {
        history.add(HistoryEntry(turn, phase, GameText(message, names.toList())))
    }

    /***
     * Is the role awake at night ? During the night, a change waiting for the end counts.
     */
    fun shownAwake(c: CharacterGame): Boolean {
        pendingNocturnal[c]?.let { return it }
        return if (phase == EnumPhase.NIGHT) nightSnapshot[c] ?: c.isNocturnal else c.isNocturnal
    }

    /** The deaths block, where the players that die at the next turn change are listed */
    fun deathsBlock(): EffectBlock? = effectBlocks.firstOrNull { it.kind == EffectBlock.Kind.DEATHS }
}
