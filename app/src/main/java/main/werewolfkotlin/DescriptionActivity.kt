package main.werewolfkotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityDescriptionBinding
import model.CharacterGame

class DescriptionActivity: AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityDescriptionBinding


    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Update content on select item
        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Retrieve the selected item
                val selectedItem = parent?.getItemAtPosition(position).toString()

                //Select the character
                val mapOfRole = WorkerEasier.characterListType.first { it.first == selectedItem }.second

                // Perform your action
                binding.imageRole.setImageDrawable(setImagePicture(mapOfRole.values.first()).drawable)

                setSpinnerType(mapOfRole)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

        // Data for the dropdown list
        val options = WorkerEasier.characterListType.map { it.first }.toList()

        // Create an ArrayAdapter
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, // Layout for each item
            options
        )

        // Set dropdown style for the spinner items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Bind the adapter to the Spinner
        binding.roleSpinner.adapter = adapter

        binding.creationButton.setOnClickListener {
            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, CreationActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }


        binding.resetButton.setOnClickListener {

            // Create the dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Description")
                .setMessage("Do you wish to reset all the roles and delete the ones you created?")
                .setPositiveButton("Yes") { _, _->
                    WorkerEasier.resetCharacters(this)
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.returnButton.setOnClickListener {
            finish()
        }
    }

    ///
    /// This function apply a picture with an image view
    ///
    private fun setImagePicture(characterGame : CharacterGame): ImageView {
        //Set the resource and image
        val imageView = ImageView(this).apply {
            @DrawableRes val img = WorkerEasier.getCharacterImage(characterGame)
            setImageResource(img)
            adjustViewBounds = true
        }

        return imageView
    }

    ///
    /// Define the second spinner on type of roles
    ///
    private fun setSpinnerType(characterMap : Map<String, CharacterGame>) {

        val roles = characterMap.keys.toList()

        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, // Layout for each item
            roles
        )

        binding.typeSpinner.adapter = arrayAdapter

        binding.typeSpinner.isEnabled = roles.size > 1

        if(roles.size > 1) {
            binding.typeSpinner.isEnabled = true

            binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Retrieve the selected item
                    val selectedItem = parent?.getItemAtPosition(position).toString()

                    //Select the character
                    binding.descriptionText.text = characterMap[selectedItem]!!.description

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: Handle the case where no item is selected
                }
            }

        } else {
            binding.descriptionText.text = characterMap.values.first().description
            binding.typeSpinner.onItemSelectedListener = null //Necessary to not activate the previous listener
            binding.typeSpinner.isEnabled = false //No selection if single item
        }

    }

}