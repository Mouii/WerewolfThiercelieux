package main.werewolfkotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class WorkerEasier {

    companion object {

        //Default asset source name
        private val sourceFilename : String = "Roles-default.json"

        //Will have the complete JSON path for the roles
        private lateinit var completeJSONPath : String

        //List of languages handle by the application
        val listLang : List<String> = listOf("en", "fr")

        @Serializable
        //Data class for association with the json file
        data class CharacterJSON (
            val description : String,
            val action : String,
            val isSolo : Boolean,
            val isNocturnal : Boolean,
            val powerState: String,
            val condition: String,
            val isWerewolf: Boolean,
            val rolesToStick: Array<String>,
            val maxOccurrence: Int
        ) {
            //Rewriting of the equals function
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as CharacterJSON

                if (description != other.description) return false
                if (action != other.action) return false
                if (isNocturnal != other.isNocturnal) return false
                if (powerState != other.powerState) return false
                if (condition != other.condition) return false
                if (isWerewolf != other.isWerewolf) return false
                if (!rolesToStick.contentEquals(other.rolesToStick)) return false
                if (maxOccurrence != other.maxOccurrence) return false

                return true
            }

            //Rewriting of the hashcode. Necessary for the data class to be valid
            override fun hashCode(): Int {
                var result = description.hashCode()
                result = 31 * result + action.hashCode()
                result = 31 * result + isNocturnal.hashCode()
                result = 31 * result + powerState.hashCode()
                result = 31 * result + condition.hashCode()
                result = 31 * result + isWerewolf.hashCode()
                result = 31 * result + rolesToStick.contentHashCode()
                result = 31 * result + maxOccurrence
                return result
            }
        }

        @Serializable
        //Another data class that simulate the association
        data class RoleJSON (
            val nameRole: String,
            val roleValues: MutableMap<String, CharacterJSON>
        )

        //MOST IMPORTANT LIST! IT'S THE LIST OF THE CHARACTERS IN THE GAME
        var characterListType : MutableList<Pair<String, MutableMap<String, CharacterGame>>> = mutableListOf()

        //Maps containing the translation. Done one time in order to avoid multiple call of translation
        var mapPowerTranslation : MutableMap<PowerState, String> = mutableMapOf()
        var mapConditionTranslation : MutableMap<ConditionalActivation, String> = mutableMapOf()


        /***
         * Function that delete all existing characters in the file read by the app
         * and reload the default one from the json in the assets
         */
        fun resetCharacters(context : Context): Boolean {

            //Copy of name. The sourceFilename must stay fixed
            var sourceFile = sourceFilename

            val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

            //Return by default en!
            val lang = sharedPreferences.getString("language", "en")!!

            //If the language is handled
            if(listLang.contains(lang) && lang != "en")
                sourceFile = replaceNameFileWithLang(sourceFile, lang)

            return try {
                context.assets.open(sourceFile).use { inputStream ->

                    val file = File(completeJSONPath)

                    if(file.exists()) {
                        file.delete()
                    }

                    file.parentFile?.mkdirs()
                    file.createNewFile()

                    characterListType.clear()

                    FileOutputStream(file.path).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    setCharactersFromJson(completeJSONPath, context, true)

                }
                true // Successful copy
            } catch (e: Exception) {
                e.printStackTrace()
                false // Failed copy
            }
        }

        /***
         * Main function to import the characters from the json
         * into the list used by the app
         */
        fun setCharactersFromJson(filename : String, context: Context, cameFromReset : Boolean = false) {

            if(!cameFromReset) {
                completeJSONPath = filename

                val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

                //Return by default en!
                val lang = sharedPreferences.getString("language", "en")!!

                if(listLang.contains(lang) && lang != "en") {
                    completeJSONPath = replaceNameFileWithLang(completeJSONPath, lang)
                }
            }

            val jsonString = File(completeJSONPath).bufferedReader().use { it.readText() }

            //Getting the complete json
            val json = Json.decodeFromString<List<RoleJSON>>(jsonString)

            //For each role
            json.forEach { roleObject ->

                val mappedCharacter: MutableMap<String, CharacterGame> = mutableMapOf()

                roleObject.roleValues.forEach { (key, value) ->

                    val characterGame = CharacterGame(
                        getStringByKey(roleObject.nameRole, context),
                        value.description,
                        value.action,
                        value.isSolo,
                        value.isNocturnal,
                        PowerState.valueOf(value.powerState),
                        ConditionalActivation.valueOf(value.condition),
                        value.isWerewolf,
                        value.rolesToStick,
                        0,
                        value.maxOccurrence,
                        key
                    )

                    mappedCharacter[key] = getGoodCharacterCast(characterGame, roleObject.nameRole)

                }

                val pair : Pair<String, MutableMap<String, CharacterGame>> = Pair(roleObject.nameRole, mappedCharacter)

                //Adding the pair to the list
                characterListType.add(pair)

            }

            //Adding translations of powers
            PowerState.entries.forEach { power ->
                mapPowerTranslation[power] = getStringByKey("Power_".plus(power.toString()), context)
            }

            //Adding translation of conditions
            ConditionalActivation.entries.forEach { condition ->
                mapConditionTranslation[condition] = getStringByKey("Condition_".plus(condition.toString()), context)
            }

        }

        /***
         * Add the fitting suffix language in order to get the good file
         */
        private fun replaceNameFileWithLang(filename : String, lang : String): String {
            val lastIndex = filename.lastIndexOf("/")
            val firstPart = filename.substring(0, lastIndex + 1)
            var realName = filename.substring(lastIndex + 1 )
            realName = realName.replace(".json", "-".plus(lang).plus(".json"))

            return firstPart.plus(realName)
        }

        /***
         * Allow getting a string resource by a string name
         */
        fun getStringByKey(key : String, context: Context) : String {
            val idRes = context.resources.getIdentifier(key, "string", context.packageName)

            return if (idRes != 0) context.getString(idRes) else ""

        }

        /***
         * When resetting language, we reset the path and context
         */
        fun resetDataSaved() {
            //Reset important values for restarting
            completeJSONPath = ""
        }

        /***
         * Add a new character even from edition mode
         */
        fun addOrUpdateNewCharacter(character: CharacterGame, role: String, type: String) {
            val mapValue = characterListType.first { it.first == role }.second
            mapValue[type] = character

            val jsonString = File(completeJSONPath).bufferedReader().use { it.readText() }

            //Getting the complete json
            val json = Json.decodeFromString<List<RoleJSON>>(jsonString)

            val jsonRoleMap = json.first { it.nameRole == role }.roleValues

            jsonRoleMap[type] = CharacterJSON(
                character.description,
                character.action,
                character.isSolo,
                character.isNocturnal,
                character.powerState.toString(),
                character.condition.toString(),
                character.isWerewolf,
                character.rolesToStick,
                character.maxOccurrence
            )

            val updateJSON = File(completeJSONPath)

            //Encode back
            updateJSON.writeText(Json.encodeToString(json))
        }

        /***
         * Delete a character
         * Note that it can't delete the characters that were wrote by default
         * except on hacking
         */
        fun deleteCharacterFromList(character: CharacterGame) {
            val mapCharacter = characterListType.first { it.first == character.className }.second

            mapCharacter.remove(character.mode)

            val jsonString = File(completeJSONPath).bufferedReader().use { it.readText() }

            //Getting the complete json
            val json = Json.decodeFromString<List<RoleJSON>>(jsonString)

            val jsonRoleMap = json.first { it.nameRole == character.className }.roleValues

            jsonRoleMap.remove(character.mode)

            val updateJSON = File(completeJSONPath)

            //Encode back
            updateJSON.writeText(Json.encodeToString(json))
        }

        /**
         * Reset important data for translation
         */
        fun resetForTranslation() {
            characterListType.clear()
            mapPowerTranslation.clear()
            mapConditionTranslation.clear()
        }

        /***
         * Regenerate the good object character from its role
         */
        fun getGoodCharacterCast(character : CharacterGame, role: String) : CharacterGame {
            when(role) {
                "Actor" -> return Actor(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Angel" -> return Angel(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "BearTamer" -> return BearTamer(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "BigBadWolf" -> return  BigBadWolf(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Brother" -> return Brother(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Crow" -> return Crow(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Cupid" -> return Cupid(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Defender" -> return Defender(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Elder" -> return Elder(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Fireman" -> return Fireman(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Fox" -> return Fox(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Gypsy" -> return Gypsy(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Hunter" -> return Hunter(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Idiot" -> return Idiot(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "LittleGirl" -> return LittleGirl(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Manipulator" -> return Manipulator(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Piper" -> return Piper(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "RustyKnight" -> return RustyKnight(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Scapegoat" -> return Scapegoat(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Seer" -> return Seer(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Servant" -> return Servant(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Sister" -> return Sister(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "StutteringJudge" -> return StutteringJudge(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Thief" -> return Thief(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Villager" -> return Villager(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "VillagerVillager" -> return VillagerVillager(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Werewolf" -> return Werewolf(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "WhiteWerewolf" -> return WhiteWerewolf(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "WildChild" -> return WildChild(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "Witch" -> return Witch(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "WolfFather" -> return WolfFather(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
                "WolfHound" -> return WolfHound(character.name
                    , character.description
                    , character.action
                    , character.isSolo
                    , character.isNocturnal
                    , character.powerState
                    , character.condition
                    , character.isWerewolf
                    , character.rolesToStick
                    , character.order
                    , character.maxOccurrence
                    , character.mode)
            }

            //Can't happened, just need here
            return character
        }

        /***
         * Return the associate picture from the object itself
         */
        fun getCharacterImage(classObject : CharacterGame) : Int {
            when(classObject) {
                is Actor -> {
                    return R.drawable.actor
                }
                is Angel -> {
                    return R.drawable.angel
                }
                is BearTamer -> {
                    return R.drawable.beartamer
                }
                is BigBadWolf -> {
                    return R.drawable.bigbadwolf
                }
                is Brother -> {
                    return R.drawable.threebrothers
                }
                is Crow -> {
                    return R.drawable.crow
                }
                is Cupid -> {
                    return R.drawable.cupid
                }
                is Defender -> {
                    return R.drawable.defender
                }
                is Elder -> {
                    return R.drawable.elder
                }
                is Fireman -> {
                    return R.drawable.fireman
                }
                is Fox -> {
                    return R.drawable.fox
                }
                is Gypsy -> {
                    return R.drawable.gypsy
                }
                is Hunter -> {
                    return R.drawable.hunter
                }
                is Idiot -> {
                    return R.drawable.villageidiot
                }
                is LittleGirl -> {
                    return R.drawable.littlegirl
                }
                is Manipulator -> {
                    return R.drawable.manipulator
                }
                is Piper -> {
                    return R.drawable.piper
                }
                is RustyKnight -> {
                    return R.drawable.rustyknight
                }
                is Scapegoat -> {
                    return R.drawable.scapegoat
                }
                is Seer -> {
                    return R.drawable.seer
                }
                is Servant -> {
                    return R.drawable.servant
                }
                is Sister -> {
                    return R.drawable.twosister
                }
                is StutteringJudge -> {
                    return R.drawable.stutteringjudge
                }
                is Thief -> {
                    return R.drawable.thief
                }
                is Villager -> {
                    return R.drawable.simplevillager
                }
                is VillagerVillager -> {
                    return R.drawable.villagervillager
                }
                is Werewolf -> {
                    return R.drawable.werewolf
                }
                is WhiteWerewolf -> {
                    return R.drawable.whitewerewolf
                }
                is WildChild -> {
                    return R.drawable.wildchild
                }
                is Witch -> {
                    return R.drawable.witch
                }
                is WolfFather -> {
                    return R.drawable.wolffather
                }
                is WolfHound -> {
                    return R.drawable.wolfhound
                }
            }

            return 0
        }

        /***
         * Handle background images for the game
         */
        fun getBackgroundImage(phase: EnumPhase) : Int {
            return when(phase) {
                EnumPhase.SLEEPING -> R.drawable.game_activity_night
                EnumPhase.NIGHT -> R.drawable.game_activity_night
                EnumPhase.DAWN -> R.drawable.game_activity_dawn
                EnumPhase.DAY -> R.drawable.game_activity_day
                EnumPhase.SUNSET -> R.drawable.game_activity_evening
            }

        }
    }
}
