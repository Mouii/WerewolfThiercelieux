package model

/***
 * The boxes of the game. A role belongs to one box, and only the roles of the activated boxes
 * can be given to the players.
 *
 * Which box a role comes from is written in the roles file, not here : see RolesRepository.
 * The name shown to the moderator is a text of the application, see ui.Labels.
 */
enum class Extension(val id: String) {
    MAIN("main"),
    NEW_MOON("new_moon"),
    VILLAGE("village"),
    CHARACTERS("characters"),
    YEARS_25("years_25"),

    /** The roles the moderator wrote himself */
    CUSTOM("custom");

    companion object {
        /***
         * Box from its name in a roles file. An unknown name is a role of the moderator.
         */
        fun parse(name: String): Extension =
            entries.firstOrNull { it.name == name.uppercase() } ?: CUSTOM
    }
}
