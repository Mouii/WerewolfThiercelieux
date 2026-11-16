package main.werewolfkotlin.ui.activity

import main.werewolfkotlin.R
import main.werewolfkotlin.data.RolesRepository
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.adapter.SpinnerAdapter
import main.werewolfkotlin.ui.adapter.SpinnerPairAdapter
import main.werewolfkotlin.ui.design.RoleImages

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityDescriptionBinding
import model.CharacterGame

class DescriptionActivity: AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityDescriptionBinding

    //Specials keys list
    //Only the roles the moderator wrote can be changed or removed

    //Selected character in the view
    private var selectedCharacter : CharacterGame? = null



    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Data for the dropdown list
        val options = RolesRepository.roleTypes.map { x-> Pair(x.first, x.second.values.first().name) }.toList()

        val roleAdapter = SpinnerPairAdapter(this, options)

        // Bind the adapter to the Spinner
        binding.roleSpinner.adapter = roleAdapter

        //Update content on select item
        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Retrieve the selected item
                val selectedItem = roleAdapter.getValue(position)

                //Select the character
                val mapOfRole = RolesRepository.roleTypes.first { it.first == selectedItem }.second

                setSpinnerType(mapOfRole)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

        binding.creationButton.setOnClickListener {
            startCreationActivity(false)
        }

        binding.editionButton.setOnClickListener {
            startCreationActivity(true)
        }

        binding.deleteButton.setOnClickListener {
            // Create the dialog
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.DescriptionView_DialogTitle))
                .setMessage(getString(R.string.DescriptionView_DeleteQuestion))
                .setPositiveButton(getString(R.string.Generic_Yes)) { _, _->
                    selectedCharacter?.let { RolesRepository.deleteCustom(this, it) }
                    recreate()
                }
                .setNegativeButton(getString(R.string.Generic_Cancel), null)
                .show()
        }

        binding.resetButton.setOnClickListener {

            // Create the dialog
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.DescriptionView_DialogTitle))
                .setMessage(getString(R.string.DescriptionView_ResetQuestion))
                .setPositiveButton(getString(R.string.Generic_Yes)) { _, _->
                    RolesRepository.deleteAllCustom(this@DescriptionActivity)
                    recreate()
                }
                .setNegativeButton(getString(R.string.Generic_Cancel), null)
                .show()
        }

        binding.returnButton.setOnClickListener {
            finish()
        }
    }

    /***
     * Execute on restart function
     */
    override fun onRestart() {
        super.onRestart()
        recreate()
    }

    /***
     * Start the creation activity
     */
    private fun startCreationActivity(editMode: Boolean) {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, CreationActivity::class.java)

        //The role to change is named, not copied : the creation screen reads it from the roles file
        if (editMode) {
            selectedCharacter?.let {
                intent.putExtra(CreationActivity.EXTRA_ROLE_NAME, it.className)
                intent.putExtra(CreationActivity.EXTRA_ROLE_KEY, it.key)
            }
        }


        //Start the other activity
        startActivity(intent)
    }

    /***
     * This function apply a picture with an image view
     */

    /***
     * Define the second spinner on type of roles
     */
    private fun setSpinnerType(characterMap : Map<String, CharacterGame>) {

        //The moderator sees the name of each variant, never its key
        val variants = characterMap.values.toList()

        binding.typeSpinner.adapter = SpinnerAdapter(this, variants.map { it.name })
        binding.typeSpinner.isEnabled = variants.size > 1 //No selection if single item

        binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val character = variants.getOrNull(position) ?: return
                selectedCharacter = character

                binding.imageRole.setImageResource(RoleImages.of(character))

                updateInformation(character)

                activateDeactivateButtons(character)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }
    }

    /***
     * Rewrite the complete information on the selected role
     */
    private fun updateInformation(character: CharacterGame) {
        binding.descriptionText.text = character.description

        val solo = if (character.isSolo) getString(R.string.DescriptionView_statDescriptionSoloYes)
        else getString(R.string.DescriptionView_statDescriptionSoloNo)

        val wakesUp = if (character.isNocturnal) getString(R.string.DescriptionView_statDescriptionWakeUpYes)
        else getString(R.string.DescriptionView_statDescriptionWakeUpNo)

        val power = String.format(
            getString(R.string.DescriptionView_statDescriptionPower),
            Labels.power(this, character.powerState)
        )

        val condition = String.format(
            getString(R.string.DescriptionView_statDescriptionCondition),
            Labels.condition(this, character.condition)
        )

        binding.attributeText.text = String.format("%s %s %s %s", solo, wakesUp, power, condition)
    }

    /***
     * Handle the buttons of edition and deletion
     */
    private fun activateDeactivateButtons(character: CharacterGame) {
        binding.editionButton.isEnabled = RolesRepository.isEditable(character)
        binding.deleteButton.isEnabled = RolesRepository.isEditable(character)
    }

}