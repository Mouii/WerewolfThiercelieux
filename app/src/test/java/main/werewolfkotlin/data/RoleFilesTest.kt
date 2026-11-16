package main.werewolfkotlin.data

import model.ConditionalActivation
import model.Extension
import model.PowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/***
 * The roles files of the application themselves : they must be readable and coherent.
 * A mistake typed in a file is caught here, before it reaches a phone.
 */
class RoleFilesTest {

    //The tests run from the folder of the module, so the files are read where they are
    private val assets = File("src/main/assets")

    private fun source(name: String): RoleSourceJSON = RolesRepository.decode(File(assets, name).readText())

    @Before
    fun readEverySource() {
        RolesRepository.clear()
        RolesRepository.fill(source("Roles-default.json"), "en")
        RolesRepository.fill(source("Roles-author.json"), "en")
    }

    @Test
    fun `the roles of the game are all there`() {
        assertEquals(36, source("Roles-default.json").roles.size)
        assertEquals(36, RolesRepository.roleTypes.size)
    }

    @Test
    fun `every text exists in every language`() {
        for (file in listOf("Roles-default.json", "Roles-author.json")) {
            for (role in source(file).roles) {
                for (language in listOf("en", "fr")) {
                    val texts = role.texts[language]
                    assertTrue("${role.nameRole} has no text in $language", texts != null)
                    assertFalse("${role.nameRole} has no name in $language", texts!!.name.isBlank())
                    assertFalse("${role.nameRole} has no description in $language", texts.description.isBlank())
                    assertFalse("${role.nameRole} has no action in $language", texts.action.isBlank())
                }
            }
        }
    }

    @Test
    fun `two variants of a role never share a name`() {
        val names = listOf("Roles-default.json", "Roles-author.json")
            .flatMap { file -> source(file).roles }
            .map { it.nameRole + "/" + RolesRepository.referenceName(it) }

        assertEquals("the name tells the variants of a role apart", names.size, names.distinct().size)
    }

    @Test
    fun `a role keeps the variants of every source`() {
        val cupid = RolesRepository.variantsOf("Cupid")!!

        assertEquals(setOf("Cupid", "Cupid Throuple", "Cupid Hanger Out"), cupid.keys)
        assertEquals("NORMAL", cupid["Cupid"]!!.source)
        assertEquals("AUTHOR", cupid["Cupid Throuple"]!!.source)
    }

    @Test
    fun `the key of a variant does not follow the language`() {
        RolesRepository.clear()
        RolesRepository.fill(source("Roles-default.json"), "fr")
        RolesRepository.fill(source("Roles-author.json"), "fr")

        val variant = RolesRepository.prototypeOf("Cupid", "Cupid Throuple")!!

        assertEquals("a saved game finds it whatever the language", "Cupid Throuple", variant.key)
        assertEquals("the moderator reads it in his language", "Cupidon Trouple", variant.name)
    }

    @Test
    fun `a variant never changes the box of its role`() {
        assertEquals(Extension.MAIN, RolesRepository.extensionOf("Cupid"))
        assertEquals(Extension.NEW_MOON, RolesRepository.extensionOf("Actor"))
    }

    @Test
    fun `a conditional role has a condition, and only a conditional role has one`() {
        for ((name, variants) in RolesRepository.roleTypes) {
            for ((key, role) in variants) {
                val conditional = role.powerState == PowerState.CONDITIONAL
                val waits = role.condition != ConditionalActivation.NO_CONDITION
                assertEquals("$name ($key)", conditional, waits)
            }
        }
    }

    @Test
    fun `one single role carries the turn of the pack`() {
        val carriers = RolesRepository.roleTypes.flatMap { it.second.values }.filter { it.definesPackTurn }

        assertEquals(listOf("Werewolf"), carriers.map { it.className })
    }

    @Test
    fun `no word of the files is unknown to the application`() {
        val powers = PowerState.entries.map { it.name }
        val conditions = ConditionalActivation.entries.map { it.name }

        for (file in listOf("Roles-default.json", "Roles-author.json")) {
            for (role in source(file).roles) {
                assertTrue("${role.nameRole} : power ${role.powerState}", role.powerState in powers)
                assertTrue("${role.nameRole} : condition ${role.condition}", role.condition in conditions)
            }
        }
    }

    @Test
    fun `an old name of a power is still understood`() {
        assertEquals(PowerState.SETUP, RolesRepository.parsePower("UNIQUE"))
        assertEquals(PowerState.PERMANENT, RolesRepository.parsePower("A_WORD_NOBODY_KNOWS"))
        assertEquals(ConditionalActivation.NO_CONDITION, RolesRepository.parseCondition("TWO-TURNS"))
    }
}
