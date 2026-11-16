package model

/***
 * What a role becomes when its condition is met.
 *
 * Only the values written here change, the others are left alone. A role that has no change
 * simply wakes up again at the state it had when it was created.
 */
class RoleChange(
    /** Does the change happen only the first time the condition is met, or every time ? */
    val fires: Firing = Firing.ONCE,

    val isNocturnal: Boolean? = null,
    val isWerewolf: Boolean? = null,
    val isSolo: Boolean? = null,
    val doTargeting: Boolean? = null,
    val doKilling: Boolean? = null,
    val powerState: PowerState? = null,
    val playsAt: NightSlot? = null,

    /** The role takes the behaviour of the role held by the player it chose (see CharacterGame.copyBehaviourOf) */
    val copiesTarget: Boolean = false
) {
    enum class Firing { ONCE, EVERY_TIME }

    /***
     * Write the values of the change on the role
     */
    fun applyTo(role: CharacterGame) {
        isNocturnal?.let { role.isNocturnal = it }
        isWerewolf?.let { role.isWerewolf = it }
        isSolo?.let { role.isSolo = it }
        doTargeting?.let { role.doTargeting = it }
        doKilling?.let { role.doKilling = it }
        powerState?.let { role.powerState = it }
        playsAt?.let { role.playsAt = it }
    }
}
