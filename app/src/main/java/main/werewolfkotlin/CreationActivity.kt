package main.werewolfkotlin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityCreationBinding
import model.CharacterGame
import model.PowerState
import java.util.Locale

class CreationActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityCreationBinding

    private lateinit var mapValues : MutableMap<Int, String>

    private val roleInt : Int = "Role".hashCode()

    private fun createSharedTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                // Get the current EditText using binding's focused child (or track focus manually)
                val currentEditText = currentFocus as? EditText

                addToText(currentEditText!!.id, currentEditText.text.toString())
            }
        }
    }

    private fun createSharedItemSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                position: Int, id: Long ) {
                val spinnerId = parent?.id // Get spinner ID
                val selectedItem = parent?.getItemAtPosition(position).toString() // Get selected value

                if (spinnerId != null) {
                    addToText(spinnerId, selectedItem)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case when no item is selected, if needed
            }
        }
    }

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreationBinding.inflate(layoutInflater)
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

                mapValues[roleInt] = selectedItem

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

        // Setup dropdown role and image
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

        binding.imageRole.setImageDrawable(setImagePicture(WorkerEasier.characterListType.first().second.values.first()).drawable)

        val idList = listOf( binding.nocturneSpinner.id, binding.werewolfSpinner.id
            , binding.categoryEdit.id, binding.powerSpinner.id, binding.actionEdit.id
            , binding.descriptionEdit.id)

        mapValues = idList.associateWith { "" }.toMutableMap()

        //List of editText
        val listEdit = listOf(
            binding.categoryEdit
            ,binding.actionEdit
            ,binding.descriptionEdit
        )

        //Object watcher
        val sharedTextWatcher = createSharedTextWatcher()

        //Affectation
        listEdit.forEach { editText ->

            editText.addTextChangedListener(sharedTextWatcher)
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                    true
                } else false
            }

            editText.imeOptions = EditorInfo.IME_ACTION_NONE
        }

        val listSpinner = listOf(
            binding.werewolfSpinner
            , binding.nocturneSpinner
            , binding.powerSpinner
        )

        listSpinner.forEach { spinner ->
            spinner.onItemSelectedListener = createSharedItemSelectedListener()
        }

        val listYN = listOf("", "YES", "NO")

        binding.werewolfSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listYN
        )

        binding.nocturneSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listYN
        )

        binding.powerSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PowerState.entries.toList()
        )

        binding.validButton.setOnClickListener {
            createNewRole()
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

    private fun createNewRole() {

        val descriptionRole = binding.descriptionEdit.text.toString()
        val actionRole = binding.actionEdit.text.toString()
        val nocturneRole = binding.nocturneSpinner.selectedItem.toString() == "YES"
        val isWerewolfRole = binding.werewolfSpinner.selectedItem.toString() == "YES"
        val powerRole = PowerState.valueOf(binding.powerSpinner.selectedItem.toString())
        val baseRole = binding.roleSpinner.selectedItem.toString()
        val keyTypeRole = binding.categoryEdit.text.toString().uppercase(Locale.getDefault())

        val newCharacterGame = CharacterGame(
            descriptionRole,
            actionRole,
            nocturneRole,
            powerRole,
            isWerewolfRole,
            0,
            1,
            keyTypeRole
        )

        val newCharacter = WorkerEasier.getGoodCharacterCast(newCharacterGame, baseRole)

        if(!WorkerEasier.characterListType.any { it.first == baseRole && it.second.containsKey(keyTypeRole) }) {
            WorkerEasier.addNewCharacter(newCharacter, baseRole, keyTypeRole)
        }

        finish()
    }

    private fun addToText(key: Int, value: String){
        mapValues[key] = value

        binding.validButton.isEnabled = !mapValues.values.any { it == "" }
    }

}