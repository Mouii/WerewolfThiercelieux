package model

/***
 * A role of the game, as it is described in the roles file, and as it is given to a player.
 *
 * There is only one class for every role : a role is described by its data, not by its own class.
 * [className] is the identity of the role ("Seer", "Werewolf"...). A role can have several variants :
 * [key] tells them apart, and [source] only says where a variant comes from (the roles of the game,
 * the variants of the author, the ones the moderator wrote).
 */
open class CharacterGame(
    //Identity of the role. Two roles with the same className are the same role
    val className: String,
    var name: String,
    var description: String,
    var action: String,
    var powerState: PowerState,
    var isNocturnal: Boolean,
    var isWerewolf: Boolean,
    var isSolo: Boolean,
    var maxOccurrence: Int,
    var condition: ConditionalActivation,
    //Does the role choose players ? If not, it has no block of targets and no lights
    var doTargeting: Boolean,
    //Does the target of the role die ? If it does, the target is a death. If not, it is a simple targeting
    var doKilling: Boolean,
    //Where the variant comes from : the key of its roles file ("NORMAL", "AUTHOR", "CUSTOM")
    var source: String,

    // ── How the role takes part in the night ───────────────────────

    /** Called at its own moment, or with the pack of werewolves */
    var playsAt: NightSlot = NightSlot.OWN_MOMENT,

    /** The role eats with the pack and then once alone : it keeps a moment of its own */
    var hasSeparateTurn: Boolean = false,

    /** The block of this role holds the players it infected, not the players it chose */
    var ownsInfectionBlock: Boolean = false,

    /** The moment of this role in the night is also the moment of the pack of werewolves */
    var definesPackTurn: Boolean = false,

    /** What the role becomes when its condition is met. Null : it only wakes up again */
    var becomes: RoleChange? = null,

    // ── Details of its power ───────────────────────────────────────

    /** Turn the condition TURN_REACHED waits for */
    var conditionTurn: Int = 0,

    /** The players it chose stay chosen night after night (the charmed players of the piper) */
    var keepsTargets: Boolean = false,

    /** It chooses among the dead players instead of the living ones (the necromancer) */
    var targetsDead: Boolean = false,

    /***
     * What tells this variant apart from the other variants of its role : its name, in the
     * reference language of its roles file. It never follows the language of the phone, so a saved
     * game still finds its roles after the moderator changed the language.
     */
    val key: String = name
) : Cloneable {

    //Moment of the role in the night. It is not a characteristic of the role : the game gives it to each role
    //given to a player, from the usual order of the roles (see RoleSequence) and the moment it plays at
    var order: Int = 0

    //Infected by the infect father of wolves : plays with the werewolves but keeps its own role
    var isInfected: Boolean = false

    //The change of the role was already applied : a change that fires once does not happen again
    var changeApplied: Boolean = false

    //Night state of the role when it was created. Useful to trigger a change of status from this state
    var originalNightState: Boolean = isNocturnal

    //Role this one copied the behaviour of (its class and the key of its variant), null when it copied nobody
    var copiedFrom: Pair<String, String>? = null

    /** Role this one behaves as : itself, or the role it copied. Sisters are alone or not by this */
    val actsAs: String get() = copiedFrom?.first ?: className

    /***
     * The role is called : text of its action.
     * A power played at the setup of the game stops waking up its player, so it is not called again.
     * A role that has a separate turn keeps its own moment and is never put to sleep here.
     */
    fun nightAction(): String {
        val playedAtSetup = powerState == PowerState.SETUP
        val conditionOver = !isWerewolf
                && powerState == PowerState.CONDITIONAL
                && condition != ConditionalActivation.IS_ALONE

        if ((playedAtSetup || conditionOver) && !hasSeparateTurn) isNocturnal = false

        return action
    }

    /***
     * Independent copy of the role : the player can change his own copy (night state, power used...)
     * without touching the role of the roles file.
     */
    public override fun clone(): CharacterGame {
        val copy = CharacterGame(
            className, name, description, action, powerState, isNocturnal, isWerewolf, isSolo,
            maxOccurrence, condition, doTargeting, doKilling, source,
            playsAt, hasSeparateTurn, ownsInfectionBlock, definesPackTurn, becomes,
            conditionTurn, keepsTargets, targetsDead, key
        )
        copy.order = order
        copy.isInfected = isInfected
        copy.changeApplied = changeApplied
        copy.originalNightState = originalNightState
        copy.copiedFrom = copiedFrom
        return copy
    }

    /***
     * Take the behaviour of another role : what it does at night, its power, its condition.
     * The identity stays (class, key, name, description), so the player keeps his own card,
     * his picture, and a saved game still finds him.
     */
    fun copyBehaviourOf(other: CharacterGame) {
        action = other.name + " : " + other.action
        powerState = other.powerState
        isNocturnal = other.isNocturnal
        originalNightState = other.originalNightState
        isWerewolf = other.isWerewolf
        isSolo = other.isSolo
        condition = other.condition
        becomes = other.becomes
        conditionTurn = other.conditionTurn
        doTargeting = other.doTargeting
        doKilling = other.doKilling
        playsAt = other.playsAt
        hasSeparateTurn = other.hasSeparateTurn
        ownsInfectionBlock = other.ownsInfectionBlock
        definesPackTurn = other.definesPackTurn
        keepsTargets = other.keepsTargets
        targetsDead = other.targetsDead
        copiedFrom = Pair(other.className, other.key)
    }

    /***
     * Copy of the role for a player, placed at this moment of the night.
     * The copy starts clean : a role given to a player is never infected, and never changed yet.
     */
    fun copyAt(order: Int): CharacterGame {
        val copy = clone()
        copy.order = order
        copy.isInfected = false
        copy.changeApplied = false
        copy.copiedFrom = null
        return copy
    }
}
