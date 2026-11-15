package model

/***
 * What a role does when the roles file of the phone does not say it (older files).
 */
object RoleDefaults {

    private val killers = setOf("Werewolf", "BigBadWolf", "WhiteWerewolf")

    private val noTarget = setOf(
        "Actor", "Angel", "BearTamer", "Brother", "Elder", "Fireman", "Hunter", "Idiot",
        "LittleGirl", "Manipulator", "Puppeteer", "RustyKnight", "Scapegoat", "Servant",
        "Sister", "StutteringJudge", "Thief", "Villager", "VillagerVillager", "WolfHound"
    )

    //The author edition of these roles does choose players
    private val authorTargets = setOf("Actor", "LittleGirl", "Manipulator", "Servant", "Thief")

    /** Roles whose target dies */
    fun doKilling(className: String): Boolean = className in killers

    /** Roles that choose players */
    fun doTargeting(className: String, mode: String): Boolean {
        //The target of the werewolves is a death : it goes in the block of the deaths
        if (doKilling(className)) return false
        if (className !in noTarget) return true
        return mode == "AUTHOR" && className in authorTargets
    }
}
