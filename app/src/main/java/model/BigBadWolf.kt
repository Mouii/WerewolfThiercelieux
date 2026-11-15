package model

class BigBadWolf(
    name: String,
    description: String,
    action: String,
    powerState: PowerState,
    isNocturnal: Boolean,
    isWerewolf: Boolean,
    isSolo: Boolean,
    maxOccurrence: Int,
    condition: ConditionalActivation,
    rolesToStick: Array<String>,
    doTargeting: Boolean,
    doKilling: Boolean,
    mode: String
) : CharacterGame(
    name,
    description,
    action,
    powerState,
    isNocturnal,
    isWerewolf,
    isSolo,
    maxOccurrence,
    condition,
    rolesToStick,
    doTargeting,
    doKilling,
    mode
) {

    /**
     * Special case for basic big bad wolf.
     * We don't want to cancel the night yet for it
     */
    override fun action(): String {
        return action
    }

    override fun clone() = BigBadWolf(name, description, action, powerState, isNocturnal, isWerewolf, isSolo, maxOccurrence, condition, rolesToStick, doTargeting, doKilling, mode).also { it.order = order }

}