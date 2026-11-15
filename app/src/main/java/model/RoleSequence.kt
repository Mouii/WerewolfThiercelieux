package model

/***
 * Usual moment of the night of each role. The moderator can move the roles during a game
 * (the new places are kept by data.RoleOrderStore), this is only the starting point.
 */
object RoleSequence {

    /** Usual order of the night. The day roles come last */
    private val defaultSequence = listOf(
        "Thief", "Actor", "Cupid", "WildChild", "Brother", "Sister", "Gypsy", "Manipulator",
        "Defender", "Fox", "Seer", "TrainedMonkey", "PowerfulMotherOfWolves",
        "Werewolf", "WolfFather", "WolfHound", "LittleGirl", "BigBadWolf", "WhiteWerewolf",
        "Witch", "Piper", "Crow",
        "Angel", "BearTamer", "Colossus", "Elder", "Fireman", "Hunter", "Idiot", "Puppeteer",
        "RustyKnight", "Scapegoat", "Servant", "StutteringJudge", "Villager", "VillagerVillager"
    )

    /** Places are spaced by 10, so two roles never share a moment unless they are linked */
    fun defaultOf(className: String): Int {
        val index = defaultSequence.indexOf(className)
        return if (index >= 0) (index + 1) * 10 else (defaultSequence.size + 1) * 10 + (className.hashCode() and 7)
    }
}
