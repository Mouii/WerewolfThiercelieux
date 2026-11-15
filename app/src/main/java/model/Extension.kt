package model

/***
 * The extensions of the game. A role belongs to one extension, and only the roles
 * of the activated extensions can be given to the players.
 * The name shown to the player is a resource, see the ui package.
 */
enum class Extension(val id: String) {
    MAIN("main"),
    NEW_MOON("new_moon"),
    VILLAGE("village"),
    CHARACTERS("characters"),
    YEARS_25("years_25"),
    CUSTOMIZE("Customize");

    companion object {
        /** Extension of each role. It is fixed, inside the application. */
        private val ofRole: Map<String, Extension> = mapOf(
            "Villager" to MAIN, "Werewolf" to MAIN, "Seer" to MAIN, "Witch" to MAIN, "Hunter" to MAIN,
            "Cupid" to MAIN, "LittleGirl" to MAIN, "Thief" to MAIN,

            "Defender" to NEW_MOON, "Elder" to NEW_MOON, "Scapegoat" to NEW_MOON, "Idiot" to NEW_MOON,
            "Piper" to NEW_MOON,

            "Crow" to VILLAGE, "Fireman" to VILLAGE, "WhiteWerewolf" to VILLAGE,

            "Actor" to CHARACTERS, "Angel" to CHARACTERS,  "Gypsy" to CHARACTERS, "BigBadWolf" to CHARACTERS,
            "Servant" to CHARACTERS, "Manipulator" to CHARACTERS,  "BearTamer" to CHARACTERS,
            "Brother" to CHARACTERS, "Sister" to CHARACTERS, "Fox" to CHARACTERS, "RustyKnight" to CHARACTERS,
            "StutteringJudge" to CHARACTERS, "VillagerVillager" to CHARACTERS, "WildChild" to CHARACTERS,
            "WolfHound" to CHARACTERS, "WolfFather" to CHARACTERS,

            "Colossus" to YEARS_25, "PowerfulMotherOfWolves" to YEARS_25, "Puppeteer" to YEARS_25,
            "TrainedMonkey" to YEARS_25
        )

        /** Extension of a role. A role that is not in the list (created by the moderator) is in the main game */
        fun of(className: String): Extension = ofRole[className] ?: CUSTOMIZE
    }
}
