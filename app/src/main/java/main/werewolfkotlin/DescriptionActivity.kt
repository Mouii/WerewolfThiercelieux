package main.werewolfkotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import main.werewolfkotlin.databinding.ActivityDescriptionBinding
import model.CharacterGame

class DescriptionActivity: AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityDescriptionBinding

    //Specials keys list
    private val listMode : List<String> = listOf("NORMAL", "AUTHOR")

    //Selected character in the view
    private var selectedCharacter : CharacterGame? = null

    class SpinnerPairAdapter(context: Context, private val items: List<Pair<String,String>>) :
        ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items.map { it.second}) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.text = items[position].second // Display the associated text
            view.gravity = android.view.Gravity.CENTER // Center the text
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.text = items[position].second // Display the associated text
            view.gravity = android.view.Gravity.CENTER // Center dropdown items' text
            return view
        }

        fun getValue(position: Int): String {
            return items[position].first // Retrieve the associated value
        }
    }

    class SpinnerAdapter(context: Context, items: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.gravity = android.view.Gravity.CENTER // Center the text
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.gravity = android.view.Gravity.CENTER // Center dropdown items' text
            return view
        }
    }

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Data for the dropdown list
        val options = WorkerEasier.characterListType.map { x-> Pair(x.first, WorkerEasier.getStringByKey(x.first)) }.toList()

        val roleAdapter = SpinnerPairAdapter(this, options)

        // Bind the adapter to the Spinner
        binding.roleSpinner.adapter = roleAdapter

        //Update content on select item
        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Retrieve the selected item
                val selectedItem = roleAdapter.getValue(position)

                //Select the character
                val mapOfRole = WorkerEasier.characterListType.first { it.first == selectedItem }.second

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
                    WorkerEasier.deleteCharacterFromList(selectedCharacter!!)
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
                    WorkerEasier.resetCharacters()
                    recreate()
                }
                .setNegativeButton(getString(R.string.Generic_Cancel), null)
                .show()
        }

        binding.resetButton.isEnabled = WorkerEasier.characterListType.any { it.second.keys.any { key -> !listMode.contains(key) } }

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

        /*if(editMode) {
            val gsonCharacter = Gson().toJson(selectedCharacter)
            intent.putExtra("CharacterEdition", gsonCharacter)
        }*/


        //Start the other activity
        startActivity(intent)
    }

    /***
     * This function apply a picture with an image view
     */
    private fun setImagePicture(characterGame : CharacterGame): ImageView {
        //Set the resource and image
        val imageView = ImageView(this).apply {
            @DrawableRes val img = WorkerEasier.getCharacterImage(characterGame)
            setImageResource(img)
            adjustViewBounds = true
        }

        return imageView
    }

    /***
     * Define the second spinner on type of roles
     */
    private fun setSpinnerType(characterMap : Map<String, CharacterGame>) {

        val roles = characterMap.keys.toList()

        binding.typeSpinner.adapter = SpinnerAdapter(this, roles)
        binding.typeSpinner.isEnabled = roles.size > 1 //No selection if single item

        binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Retrieve the selected item
                val selectedItem = parent?.getItemAtPosition(position).toString()

                selectedCharacter = characterMap[selectedItem]!!

                // Perform your action
                binding.imageRole.setImageDrawable(setImagePicture(selectedCharacter!!).drawable)

                updateInformation()

                activateDeactivateButtons(selectedItem)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }
    }

    /***
     * Rewrite the complete information on the selected role
     */
    private fun updateInformation() {
        binding.descriptionText.text = selectedCharacter!!.description

        binding.attributeText.text = String.format("%s %s %s"
        , if(selectedCharacter!!.isSolo) getString(R.string.DescriptionView_statDescriptionSoloYes)
            else getString(R.string.DescriptionView_statDescriptionSoloNo)
        , if(selectedCharacter!!.isNocturnal) getString(R.string.DescriptionView_statDescriptionWakeUpYes)
            else getString(R.string.DescriptionView_statDescriptionWakeUpNo)
        , String.format(getString(R.string.DescriptionView_statDescriptionPower), selectedCharacter!!.powerState.value))

    }

    /***
     * Handle the buttons of edition and deletion
     */
    private fun activateDeactivateButtons(mode: String) {
        binding.editionButton.isEnabled = !listMode.contains(mode.uppercase())
        binding.deleteButton.isEnabled = !listMode.contains(mode.uppercase())
    }

}