package main.werewolfkotlin.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import model.CharacterGame
import model.ConditionalActivation
import model.Extension
import model.NightSlot
import model.PowerState
import model.RoleChange
import java.io.File

/***
 * Where the roles come from.
 *
 * The roles are read from sources, in order. Each source is one file with its own key, and a role
 * can exist in several sources : those are its variants, and the moderator picks the one he wants.
 *
 *  - the roles of the game and the variants of the author are in the assets : they never change,
 *    so the application only reads them ;
 *  - the roles the moderator writes are in one file of the phone, which is the only one written to.
 *
 * A source that cannot be read is skipped : a broken optional file never stops the application,
 * and the roles the moderator wrote can never be lost by an update.
 */
object RolesRepository {

    private const val TAG = "RolesRepository"

    /** Key of the source that holds what the moderator wrote */
    const val CUSTOM_KEY = "CUSTOM"

    /** Sources read from the assets, in reading order. The first one is required */
    private val assetSources = listOf("Roles-default.json", "Roles-author.json")

    /** The only file the application writes : the roles of the moderator */
    private const val CUSTOM_FILE = "Roles-custom.json"

    //An unknown field must never stop the reading of a file
    private val jsonFormat = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // ── The roles in memory ────────────────────────────────────────

    /** The roles of the game : for each role, its variants by key (see CharacterGame.key) */
    var roleTypes: MutableList<Pair<String, MutableMap<String, CharacterGame>>> = mutableListOf()
        private set

    /** Box of the game each role comes from, set by the first source that declares it */
    private val extensions: MutableMap<String, Extension> = mutableMapOf()

    /** Language the roles in memory were read in : reading again in another language is needed */
    private var loadedLanguage: String = ""

    val isLoaded: Boolean get() = roleTypes.isNotEmpty()

    /** The variants of a role, by source key */
    fun variantsOf(className: String): MutableMap<String, CharacterGame>? =
        roleTypes.firstOrNull { it.first == className }?.second

    /** A precise variant of a role, from its key */
    fun prototypeOf(className: String, key: String): CharacterGame? = variantsOf(className)?.get(key)

    /** Box of the game a role comes from. A role of the moderator belongs to his own box */
    fun extensionOf(className: String): Extension = extensions[className] ?: Extension.CUSTOM

    /** A role can be changed or removed : only the roles the moderator wrote can */
    fun isEditable(character: CharacterGame): Boolean = character.source == CUSTOM_KEY

    /***
     * What tells a variant apart from the other variants of its role : its name in the reference
     * language of its file (the first one it is written in, English for the files of the application).
     */
    fun referenceName(role: RoleJSON): String =
        role.texts["en"]?.name?.takeIf { it.isNotBlank() }
            ?: role.texts.values.firstOrNull { it.name.isNotBlank() }?.name
            ?: role.nameRole

    // ── Loading ────────────────────────────────────────────────────

    /***
     * Read every source. The roles of the game are required, the other sources are optional.
     * False when the roles of the game could not be read : nothing can be played then.
     */
    fun load(context: Context): Boolean {
        val language = AppSettings.languageOrEnglish(context)

        roleTypes = mutableListOf()
        extensions.clear()
        loadedLanguage = language

        var first = true
        for (name in assetSources) {
            val source = readAsset(context, name)
            if (source == null) {
                if (first) {
                    Log.e(TAG, "The roles of the game cannot be read : $name")
                    return false
                }
                Log.w(TAG, "Source skipped : $name")
            } else {
                fill(source, language)
            }
            first = false
        }

        readCustom(context)?.let { fill(it, language) }

        return isLoaded
    }

    /***
     * Read the sources again when the language changed, so the texts follow
     */
    fun reloadIfLanguageChanged(context: Context) {
        if (loadedLanguage != AppSettings.languageOrEnglish(context)) load(context)
    }

    /** Everything has to be read again (the moderator changed the language) */
    fun clear() {
        roleTypes = mutableListOf()
        extensions.clear()
        loadedLanguage = ""
    }

    /***
     * A source of roles from the text of its file. Also used by the tests that check the roles files.
     */
    fun decode(text: String): RoleSourceJSON = jsonFormat.decodeFromString<RoleSourceJSON>(text)

    private fun readAsset(context: Context, name: String): RoleSourceJSON? = try {
        context.assets.open(name).use { input -> decode(input.readBytes().decodeToString()) }
    } catch (ex: Exception) {
        Log.e(TAG, "The roles file $name cannot be read", ex)
        null
    }

    private fun readCustom(context: Context): RoleSourceJSON? {
        val file = customFile(context)
        if (!file.exists()) return null
        return try {
            decode(file.readText())
        } catch (ex: Exception) {
            Log.e(TAG, "The roles of the moderator cannot be read", ex)
            null
        }
    }

    private fun customFile(context: Context) = File(context.filesDir, CUSTOM_FILE)

    /***
     * Put the roles of a source in memory. A role that is already there gains a variant.
     */
    fun fill(source: RoleSourceJSON, language: String) {
        for (role in source.roles) {
            val variants = variantsOf(role.nameRole) ?: mutableMapOf<String, CharacterGame>().also {
                roleTypes.add(Pair(role.nameRole, it))
            }
            //Two variants of a role with the same name : the second one says where it comes from
            var key = referenceName(role)
            if (variants.containsKey(key)) {
                Log.w(TAG, "Two variants of ${role.nameRole} are named $key")
                key = "$key (${source.key})"
            }
            variants[key] = build(role, source.key, key, language)

            //The first source that declares a role says which box it comes from
            if (!extensions.containsKey(role.nameRole)) {
                role.extension?.let { extensions[role.nameRole] = Extension.parse(it) }
            }
        }
    }

    /***
     * One role, read in the language of the application. A missing language falls back to English.
     */
    private fun build(role: RoleJSON, sourceKey: String, key: String, language: String): CharacterGame {
        val texts = role.texts[language] ?: role.texts["en"] ?: role.texts.values.firstOrNull()

        return CharacterGame(
            className = role.nameRole,
            name = texts?.name?.ifBlank { role.nameRole } ?: role.nameRole,
            description = texts?.description ?: "",
            action = texts?.action ?: "",
            powerState = parsePower(role.powerState),
            isNocturnal = role.isNocturnal,
            isWerewolf = role.isWerewolf,
            isSolo = role.isSolo,
            maxOccurrence = role.maxOccurrence,
            condition = parseCondition(role.condition),
            doTargeting = role.doTargeting,
            doKilling = role.doKilling,
            source = sourceKey,
            playsAt = parseSlot(role.playsAt),
            hasSeparateTurn = role.hasSeparateTurn,
            ownsInfectionBlock = role.ownsInfectionBlock,
            definesPackTurn = role.definesPackTurn,
            conditionTurn = role.conditionTurn,
            keepsTargets = role.keepsTargets,
            targetsDead = role.targetsDead,
            key = key,
            becomes = role.becomes?.let { change ->
                RoleChange(
                    fires = if (change.fires == "EVERY_TIME") RoleChange.Firing.EVERY_TIME
                    else RoleChange.Firing.ONCE,
                    isNocturnal = change.isNocturnal,
                    isWerewolf = change.isWerewolf,
                    isSolo = change.isSolo,
                    doTargeting = change.doTargeting,
                    doKilling = change.doKilling,
                    powerState = change.powerState?.let { parsePower(it) },
                    playsAt = change.playsAt?.let { parseSlot(it) },
                    copiesTarget = change.copiesTarget
                )
            }
        )
    }

    // ── Reading the named values ───────────────────────────────────

    /***
     * Power from its name. An unknown name gives a role without power rather than an error.
     * UNIQUE was the first name of SETUP and is still understood.
     */
    fun parsePower(name: String): PowerState = when (name.uppercase()) {
        "SETUP", "UNIQUE" -> PowerState.SETUP
        "CONSUMABLE" -> PowerState.CONSUMABLE
        "CONDITIONAL" -> PowerState.CONDITIONAL
        "PERMANENT" -> PowerState.PERMANENT
        else -> {
            Log.w(TAG, "Unknown power : $name")
            PowerState.PERMANENT
        }
    }

    /***
     * Condition from its name. The first names of the older files are understood too.
     */
    fun parseCondition(name: String): ConditionalActivation = when (name.uppercase()) {
        "NO_CONDITION", "NOCONDITION" -> ConditionalActivation.NO_CONDITION
        "ONE_WEREWOLF", "ONEWEREWOLF" -> ConditionalActivation.ONE_WEREWOLF
        "ONE_VILLAGER", "ONEVILLAGER" -> ConditionalActivation.ONE_VILLAGER
        "ALL_WEREWOLVES", "ALLWEREWOLVES" -> ConditionalActivation.ALL_WEREWOLVES
        "ALL_VILLAGERS", "ALLVILLAGERS" -> ConditionalActivation.ALL_VILLAGERS
        "IS_ALONE", "ISALONE" -> ConditionalActivation.IS_ALONE
        "MY_TARGET_DIES" -> ConditionalActivation.MY_TARGET_DIES
        "TARGET_CHOSEN" -> ConditionalActivation.TARGET_CHOSEN
        "TURN_REACHED" -> ConditionalActivation.TURN_REACHED
        "ON_MORNING", "ONMORNING" -> ConditionalActivation.ON_MORNING
        else -> {
            Log.w(TAG, "Unknown condition : $name")
            ConditionalActivation.NO_CONDITION
        }
    }

    private fun parseSlot(name: String?): NightSlot =
        if (name != null && name.uppercase() == "WEREWOLF_TURN") NightSlot.WEREWOLF_TURN
        else NightSlot.OWN_MOMENT

    // ── The roles of the moderator ─────────────────────────────────

    /***
     * Add or replace a role of the moderator. Only his own file is written.
     */
    fun saveCustom(context: Context, character: CharacterGame) {
        val variants = variantsOf(character.className) ?: mutableMapOf<String, CharacterGame>().also {
            roleTypes.add(Pair(character.className, it))
        }

        //The moderator can write several variants of a role : their names tell them apart
        variants[character.key] = character

        writeCustom(context)
    }

    /** A variant of this role already has this name */
    fun isNameTaken(className: String, name: String): Boolean = variantsOf(className)?.containsKey(name) == true

    /***
     * Remove a role of the moderator. A role of the game or of the author cannot be removed.
     */
    fun deleteCustom(context: Context, character: CharacterGame): Boolean {
        if (!isEditable(character)) return false

        variantsOf(character.className)?.remove(character.key)
        roleTypes.removeAll { it.second.isEmpty() }

        writeCustom(context)
        return true
    }

    /***
     * Forget every role the moderator wrote
     */
    fun deleteAllCustom(context: Context): Boolean {
        val file = customFile(context)
        val removed = !file.exists() || file.delete()
        if (removed) load(context)
        return removed
    }

    /***
     * Write the file of the moderator from what is in memory
     */
    private fun writeCustom(context: Context) {
        val language = loadedLanguage.ifEmpty { "en" }

        val roles = roleTypes.flatMap { (name, variants) ->
            variants.values.filter { isEditable(it) }.map { character ->
                RoleJSON(
                    nameRole = name,
                    extension = extensions[name]?.name,
                    powerState = character.powerState.name,
                    isNocturnal = character.isNocturnal,
                    isWerewolf = character.isWerewolf,
                    isSolo = character.isSolo,
                    maxOccurrence = character.maxOccurrence,
                    doTargeting = character.doTargeting,
                    doKilling = character.doKilling,
                    condition = character.condition.name,
                    conditionTurn = character.conditionTurn,
                    becomes = character.becomes?.let { change ->
                        RoleChangeJSON(
                            fires = change.fires.name,
                            isNocturnal = change.isNocturnal,
                            isWerewolf = change.isWerewolf,
                            isSolo = change.isSolo,
                            doTargeting = change.doTargeting,
                            doKilling = change.doKilling,
                            powerState = change.powerState?.name,
                            playsAt = change.playsAt?.takeIf { it == NightSlot.WEREWOLF_TURN }?.name,
                            copiesTarget = change.copiesTarget
                        )
                    },
                    playsAt = if (character.playsAt == NightSlot.WEREWOLF_TURN) "WEREWOLF_TURN" else null,
                    hasSeparateTurn = character.hasSeparateTurn,
                    ownsInfectionBlock = character.ownsInfectionBlock,
                    definesPackTurn = character.definesPackTurn,
                    keepsTargets = character.keepsTargets,
                    targetsDead = character.targetsDead,
                    //Written in one language only : the name typed by the moderator is its own reference
                    texts = mapOf(
                        language to RoleTextsJSON(character.key, character.description, character.action)
                    )
                )
            }
        }

        try {
            val source = RoleSourceJSON(CUSTOM_KEY, AppSettings.languages, roles)
            customFile(context).writeText(jsonFormat.encodeToString(source))
        } catch (ex: Exception) {
            Log.e(TAG, "The roles of the moderator could not be written", ex)
        }
    }
}
