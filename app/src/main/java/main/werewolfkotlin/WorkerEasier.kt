package main.werewolfkotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat.getString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import java.io.File
import java.io.FileOutputStream

class WorkerEasier {

    companion object {

        private var sourceFilename : String = "Roles-default.json"

        private lateinit var completeJSONPath : String

        @SuppressLint("StaticFieldLeak")
        private var contextHolder : Context? = null

        @Serializable
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
        data class RoleJSON (
            val nameRole: String,
            val roleValues: MutableMap<String, CharacterJSON>
        )

        var characterListType : MutableList<Pair<String, MutableMap<String, CharacterGame>>> = mutableListOf()

        fun resetCharacters(context: Activity): Boolean {
            return try {
                context.assets.open(sourceFilename).use { inputStream ->

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

                    setCharactersFromJson(completeJSONPath)

                }
                true // Successful copy
            } catch (e: Exception) {
                e.printStackTrace()
                false // Failed copy
            }
        }

        fun setCharactersFromJson(filename : String, context: Context? = null) {

            if(context != null)
                contextHolder = context.applicationContext

            completeJSONPath = filename

            val jsonString = File(filename).bufferedReader().use { it.readText() }

            //Getting the complete json
            val json = Json.decodeFromString<List<RoleJSON>>(jsonString)

            //For each role
            json.forEach { roleObject ->

                val mappedCharacter: MutableMap<String, CharacterGame> = mutableMapOf()

                roleObject.roleValues.forEach { (key, value) ->

                    val characterGame = CharacterGame(
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

        }

        fun getStringByKey(key : String) : String {
            if(contextHolder != null) {
                val idRes = contextHolder!!.resources.getIdentifier(key, "string", contextHolder!!.packageName)

                return if (idRes != 0) contextHolder!!.getString(idRes) else "String not found"
            } else
                return ""

        }

        fun getString(key : Int) : String  {
            return if(contextHolder != null)
                contextHolder!!.getString(key)
            else
                ""
        }

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

        fun getGoodCharacterCast(character : CharacterGame, role: String) : CharacterGame {
            when(role) {
                "Actor" -> return Actor(character.description
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
                "Angel" -> return Angel(character.description
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
                "BearTamer" -> return BearTamer(character.description
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
                "BigBadWolf" -> return  BigBadWolf(character.description
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
                "Brother" -> return Brother(character.description
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
                "Crow" -> return Crow(character.description
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
                "Cupid" -> return Cupid(character.description
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
                "Defender" -> return Defender(character.description
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
                "Elder" -> return Elder(character.description
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
                "Fireman" -> return Fireman(character.description
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
                "Fox" -> return Fox(character.description
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
                "Gypsy" -> return Gypsy(character.description
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
                "Hunter" -> return Hunter(character.description
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
                "Idiot" -> return Idiot(character.description
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
                "LittleGirl" -> return LittleGirl(character.description
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
                "Manipulator" -> return Manipulator(character.description
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
                "Piper" -> return Piper(character.description
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
                "RustyKnight" -> return RustyKnight(character.description
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
                "Scapegoat" -> return Scapegoat(character.description
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
                "Seer" -> return Seer(character.description
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
                "Servant" -> return Servant(character.description
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
                "Sister" -> return Sister(character.description
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
                "StutteringJudge" -> return StutteringJudge(character.description
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
                "Thief" -> return Thief(character.description
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
                "Villager" -> return Villager(character.description
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
                "VillagerVillager" -> return VillagerVillager(character.description
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
                "Werewolf" -> return Werewolf(character.description
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
                "WhiteWerewolf" -> return WhiteWerewolf(character.description
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
                "WildChild" -> return WildChild(character.description
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
                "Witch" -> return Witch(character.description
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
                "WolfFather" -> return WolfFather(character.description
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
                "WolfHound" -> return WolfHound(character.description
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
