package main.werewolfkotlin.data

import kotlinx.serialization.Serializable

/***
 * What a roles file holds, field by field : this file is the format of the roles files.
 *
 * One file is one source of roles, named by its [RoleSourceJSON.key] : the roles of the game,
 * the variants of the author, the ones the moderator wrote. The key only says where a role comes
 * from. A file can hold several variants of the same role : their names tell them apart. A role is described once, and its
 * texts are given for every language of the application, so there is no file per language.
 *
 * Every value that names a choice (power, condition, moment of the night) is read as plain text
 * and converted with a default, so that one unknown word never makes a whole file unreadable.
 */
@Serializable
class RoleSourceJSON(
    //Name of the source : where the roles of this file come from
    val key: String,
    //Languages the texts of this file are written in, for information
    val languages: List<String> = emptyList(),
    val roles: List<RoleJSON> = emptyList()
)

@Serializable
class RoleJSON(
    //Identity of the role, the same in every source : "Seer", "Werewolf"...
    val nameRole: String,

    //Box of the game the role comes from. Only the first source that declares a role sets it
    val extension: String? = null,

    val powerState: String = "PERMANENT",
    val isNocturnal: Boolean = false,
    val isWerewolf: Boolean = false,
    val isSolo: Boolean = false,
    val maxOccurrence: Int = 1,
    //Does the role choose players, and does its target die ?
    val doTargeting: Boolean = false,
    val doKilling: Boolean = false,

    //What has to happen for the role to change (see ConditionalActivation)
    val condition: String = "NO_CONDITION",
    //Turn the condition TURN_REACHED waits for
    val conditionTurn: Int = 0,
    //What the role becomes then. Absent : it only wakes up again
    val becomes: RoleChangeJSON? = null,

    //"WEREWOLF_TURN" : the role is called with the pack. Absent : at its own moment
    val playsAt: String? = null,
    //The role eats with the pack and then once alone
    val hasSeparateTurn: Boolean = false,
    //The block of this role holds the players it infected
    val ownsInfectionBlock: Boolean = false,
    //The moment of this role is also the moment of the pack
    val definesPackTurn: Boolean = false,
    //The players it chose stay chosen night after night
    val keepsTargets: Boolean = false,
    //It chooses among the dead players
    val targetsDead: Boolean = false,

    //Texts of the role, by language : "en", "fr"... The name in the first language of the file
    //is what tells the variants of a role apart, so it must be unique among them
    val texts: Map<String, RoleTextsJSON> = emptyMap()
)

@Serializable
class RoleTextsJSON(
    val name: String = "",
    val description: String = "",
    val action: String = ""
)

/***
 * What a role becomes when its condition is met. Only the values written here change.
 */
@Serializable
class RoleChangeJSON(
    //"ONCE" or "EVERY_TIME"
    val fires: String = "ONCE",
    val isNocturnal: Boolean? = null,
    val isWerewolf: Boolean? = null,
    val isSolo: Boolean? = null,
    val doTargeting: Boolean? = null,
    val doKilling: Boolean? = null,
    val powerState: String? = null,
    val playsAt: String? = null,
    //The role takes the behaviour of the role held by the player it chose
    val copiesTarget: Boolean = false
)
