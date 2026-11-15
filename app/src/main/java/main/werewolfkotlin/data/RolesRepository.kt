package main.werewolfkotlin.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.CharacterGame
import model.ConditionalActivation
import model.PowerState
import model.RoleDefaults
import java.io.File

/***
 * Where the roles come from : the JSON file of the phone, copied from the assets the first time
 * (one file per language). The list of the roles of the game is kept here, in memory.
 */
object RolesRepository {

    private const val TAG = "RolesRepository"

    //Default asset source name
    private const val SOURCE_FILENAME = "Roles-default.json"

    //Where the file of the phone is
    private const val PHONE_FILE = "/json/Roles.json"

    //Will have the complete JSON path for the roles
    private var completeJSONPath: String = ""

    // ── JSON shape ─────────────────────────────────────────────────

    /** Data class for association with the json file */
    @Serializable
    data class CharacterJSON(
        val description: String,
        val action: String,
        val powerState: String,
        val isNocturnal: Boolean,
        val isWerewolf: Boolean,
        val isSolo: Boolean,
        val maxOccurrence: Int,
        val condition: String,
        val rolesToStick: List<String>,
        //Does the role choose players ? null in an old file : the usual value of the role is used
        val doTargeting: Boolean? = null,
        //Does the target of the role die ? null in an old file : the usual value of the role is used
        val doKilling: Boolean? = null,
        //Names of the first files, only read : hasTarget is doTargeting, kills is doKilling
        val hasTarget: Boolean? = null,
        val kills: Boolean? = null
    )

    /** The variants (NORMAL, AUTHOR...) of a role */
    @Serializable
    data class RoleJSON(
        val nameRole: String,
        val roleValues: MutableMap<String, CharacterJSON>
    )

    //The older files have keys that the new ones don't have : they must not stop the reading
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    /***
     * Condition from its name. The first names (NOCONDITION, ISALONE...) of the older files are understood too
     */
    fun parseCondition(name: String): ConditionalActivation = when (name) {
        "NOCONDITION" -> ConditionalActivation.NO_CONDITION
        "ONEWEREWOLF" -> ConditionalActivation.ONE_WEREWOLF
        "ONEVILLAGER" -> ConditionalActivation.ONE_VILLAGER
        "ALLWEREWOLVES" -> ConditionalActivation.ALL_WEREWOLVES
        "ALLVILLAGERS" -> ConditionalActivation.ALL_VILLAGERS
        "LINKEDROLES" -> ConditionalActivation.LINKED_PLAYERS
        "ISALONE" -> ConditionalActivation.IS_ALONE
        else -> ConditionalActivation.valueOf(name)
    }

    // ── The roles in memory ────────────────────────────────────────

    /** THE LIST OF THE CHARACTERS IN THE GAME : for each role class, its variants by mode */
    var roleTypes: MutableList<Pair<String, MutableMap<String, CharacterGame>>> = mutableListOf()
        private set

    val isLoaded: Boolean get() = roleTypes.isNotEmpty()

    /** The variants of a role class, by mode */
    fun variantsOf(className: String): MutableMap<String, CharacterGame>? =
        roleTypes.firstOrNull { it.first == className }?.second

    /** The role of the list from its class and its mode */
    fun prototypeOf(className: String, mode: String): CharacterGame? = variantsOf(className)?.get(mode)

    // ── Loading ────────────────────────────────────────────────────

    /***
     * Name of the file in the language of the application
     */
    private fun inLanguage(context: Context, filename: String): String {
        val lang = AppSettings.languageOrEnglish(context)
        if (!AppSettings.languages.contains(lang) || lang == "en") return filename

        val lastIndex = filename.lastIndexOf("/")
        val folder = filename.substring(0, lastIndex + 1)
        val name = filename.substring(lastIndex + 1).replace(".json", "-$lang.json")
        return folder + name
    }

    /** Path of the file of the phone */
    fun phoneFilePath(context: Context): String = context.filesDir.toString() + PHONE_FILE

    /***
     * Load the list of characters from the file of the phone
     */
    fun loadFromPhone(context: Context): Boolean {
        return try {
            setCharactersFromJson(phoneFilePath(context), context)
            true
        } catch (ex: Exception) {
            Log.e(TAG, "The roles file of the phone cannot be read", ex)
            false
        }
    }

    /***
     * Delete all existing characters in the file read by the app
     * and reload the default one from the json in the assets
     */
    fun resetCharacters(context: Context): Boolean {
        val sourceFile = inLanguage(context, SOURCE_FILENAME)

        return try {
            context.assets.open(sourceFile).use { inputStream ->
                val file = File(completeJSONPath)

                if (file.exists()) file.delete()
                file.parentFile?.mkdirs()
                file.createNewFile()

                roleTypes.clear()

                file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }

                setCharactersFromJson(completeJSONPath, context, true)
            }
            true // Successful copy
        } catch (ex: Exception) {
            Log.e(TAG, "The roles file could not be written from the assets", ex)
            false // Failed copy
        }
    }

    /***
     * Main function to import the characters from the json into the list used by the app
     */
    fun setCharactersFromJson(filename: String, context: Context, cameFromReset: Boolean = false) {
        if (!cameFromReset) completeJSONPath = inLanguage(context, filename)

        roleTypes.addAll(readJson().map { roleObject ->
            val variants: MutableMap<String, CharacterGame> = mutableMapOf()

            roleObject.roleValues.forEach { (mode, value) ->
                variants[mode] = CharacterGame(
                    roleObject.nameRole,
                    context.stringByKey(roleObject.nameRole),
                    value.description,
                    value.action,
                    PowerState.valueOf(value.powerState),
                    value.isNocturnal,
                    value.isWerewolf,
                    value.isSolo,
                    value.maxOccurrence,
                    parseCondition(value.condition),
                    value.rolesToStick.toTypedArray(),
                    value.doTargeting ?: value.hasTarget ?: RoleDefaults.doTargeting(roleObject.nameRole, mode),
                    value.doKilling ?: value.kills ?: RoleDefaults.doKilling(roleObject.nameRole),
                    mode
                )
            }

            Pair(roleObject.nameRole, variants)
        })
    }

    /***
     * When resetting language, we reset the path and context
     */
    fun resetDataSaved() {
        completeJSONPath = ""
    }

    /**
     * Reset important data for translation
     */
    fun resetForTranslation() {
        roleTypes.clear()
    }

    // ── Editing ────────────────────────────────────────────────────

    private fun readJson(): List<RoleJSON> =
        jsonFormat.decodeFromString(File(completeJSONPath).bufferedReader().use { it.readText() })

    private fun writeJson(json: List<RoleJSON>) {
        File(completeJSONPath).writeText(jsonFormat.encodeToString(json))
    }

    /***
     * Add a new character even from edition mode
     */
    fun addOrUpdateNewCharacter(character: CharacterGame, role: String, mode: String) {
        roleTypes.first { it.first == role }.second[mode] = character

        val json = readJson()
        json.first { it.nameRole == role }.roleValues[mode] = CharacterJSON(
            character.description,
            character.action,
            character.powerState.toString(),
            character.isNocturnal,
            character.isWerewolf,
            character.isSolo,
            character.maxOccurrence,
            character.condition.toString(),
            character.rolesToStick.toList(),
            character.doTargeting,
            character.doKilling
        )
        writeJson(json)
    }

    /***
     * Delete a character
     * Note that it can't delete the characters that were wrote by default
     * except on hacking
     */
    fun deleteCharacterFromList(character: CharacterGame) {
        roleTypes.first { it.first == character.className }.second.remove(character.mode)

        val json = readJson()
        json.first { it.nameRole == character.className }.roleValues.remove(character.mode)
        writeJson(json)
    }
}
