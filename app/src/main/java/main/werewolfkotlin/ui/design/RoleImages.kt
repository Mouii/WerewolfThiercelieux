package main.werewolfkotlin.ui.design

import main.werewolfkotlin.R
import main.werewolfkotlin.game.EnumPhase
import model.CharacterGame

/***
 * Pictures of the game : the picture of each role, the background of each phase
 */
object RoleImages {

    private val ofClass: Map<String, Int> = mapOf(
        "Actor" to R.drawable.role_actor,
        "Angel" to R.drawable.role_angel,
        "BearTamer" to R.drawable.role_beartamer,
        "BigBadWolf" to R.drawable.role_bigbadwolf,
        "Brother" to R.drawable.role_threebrothers,
        "Colossus" to R.drawable.role_colossus,
        "Crow" to R.drawable.role_crow,
        "Cupid" to R.drawable.role_cupid,
        "Defender" to R.drawable.role_defender,
        "Elder" to R.drawable.role_elder,
        "Fireman" to R.drawable.role_fireman,
        "Fox" to R.drawable.role_fox,
        "Gypsy" to R.drawable.role_gypsy,
        "Hunter" to R.drawable.role_hunter,
        "Idiot" to R.drawable.role_villageidiot,
        "LittleGirl" to R.drawable.role_littlegirl,
        "Manipulator" to R.drawable.role_manipulator,
        "Piper" to R.drawable.role_piper,
        "PowerfulMotherOfWolves" to R.drawable.role_powerfulmotherofwolves,
        "Puppeteer" to R.drawable.role_puppeteer,
        "RustyKnight" to R.drawable.role_rustyknight,
        "Scapegoat" to R.drawable.role_scapegoat,
        "Seer" to R.drawable.role_seer,
        "Servant" to R.drawable.role_servant,
        "Sister" to R.drawable.role_twosister,
        "StutteringJudge" to R.drawable.role_stutteringjudge,
        "Thief" to R.drawable.role_thief,
        "TrainedMonkey" to R.drawable.role_trainedmonkey,
        "Villager" to R.drawable.role_simplevillager,
        "VillagerVillager" to R.drawable.role_villagervillager,
        "Werewolf" to R.drawable.role_werewolf,
        "WhiteWerewolf" to R.drawable.role_whitewerewolf,
        "WildChild" to R.drawable.role_wildchild,
        "Witch" to R.drawable.role_witch,
        "WolfFather" to R.drawable.role_wolffather,
        "WolfHound" to R.drawable.role_wolfhound
    )

    /** Picture of the werewolf pack : the turn of the werewolves has no role of its own */
    val WOLF_PACK: Int = R.drawable.role_werewolf

    /** Picture of a role, 0 if there is none */
    fun of(character: CharacterGame): Int = ofClass[character.className] ?: 0

    /** Background of the game screen for this phase */
    fun background(phase: EnumPhase): Int = when (phase) {
        EnumPhase.SLEEPING -> R.drawable.bg_game_night
        EnumPhase.NIGHT -> R.drawable.bg_game_night
        EnumPhase.DAWN -> R.drawable.bg_game_dawn
        EnumPhase.DAY -> R.drawable.bg_game_day
        EnumPhase.SUNSET -> R.drawable.bg_game_evening
    }
}
