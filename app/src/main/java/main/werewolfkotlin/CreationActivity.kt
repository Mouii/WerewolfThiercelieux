package main.werewolfkotlin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import main.werewolfkotlin.databinding.ActivityCreationBinding
import model.CharacterGame
import model.ConditionalActivation
import model.PowerState
import java.util.Locale

class CreationActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityCreationBinding

    private lateinit var mapValues : MutableMap<Int, String>

    private var rolesForLink : MutableList<String> = mutableListOf()

    private var characterEdit : CharacterGame? = null

    private var editionMode : Boolean = false

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

    private fun createPairSharedItemSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                position: Int, id: Long ) {
                val spinnerId = parent?.id // Get spinner ID

                //We cast the adapter, that is from this class, to get the function that interest us => getValue()
                val selectedItem = (parent?.adapter as SpinnerPairAdapter).getValue(position)

                if (spinnerId != null) {
                    addToText(spinnerId, selectedItem)

                    if(spinnerId == binding.powerSpinner.id) {
                        binding.conditionalSpinner.isEnabled =
                            binding.powerSpinner.selectedItem == PowerState.CONDITIONAL.value
                        binding.conditionalSpinner.alpha =
                            if (binding.conditionalSpinner.isEnabled) 1.0f else 0.5f
                    }

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case when no item is selected, if needed
            }
        }
    }

    private class SpinnerAdapter(context: Context, items: List<String>) :
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

        // Retrieve the JSON string from the Intent
        val jsonString = intent.getStringExtra("CharacterEdition")

        // Convert the JSON string back to a MutableList
        val gson = Gson()

        val type = object : TypeToken<CharacterGame>() {}.type

        try {
            //Get the list from the JSON
            //Even if null, still pass
            characterEdit = gson.fromJson(jsonString, type)
            editionMode = characterEdit != null
        } catch (ex : Exception) {
            characterEdit = null
        }

        //List that will contains all the roles
        val listRoles : MutableList<Pair<String, String>> = mutableListOf()

        //List that will have all pair of role and key
        val listLink : MutableList<String> = mutableListOf()

        WorkerEasier.characterListType.forEach { x ->

            //Adding the pair
            x.second.forEach { map ->
                listLink.add(x.first.uppercase().plus("-").plus(map.key))
            }

            //Adding role
            listRoles.add(Pair(x.first, x.second.values.first().name))
        }

        val pairAdapter = SpinnerPairAdapter(this, listRoles )

        // Bind the adapter to the Spinner
        binding.roleSpinner.adapter = pairAdapter

        //Update content on select item
        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerId = parent?.id // Get spinner ID

                // Retrieve the selected item
                val selectedItem = pairAdapter.getValue(position)

                //Select the character
                val mapOfRole = WorkerEasier.characterListType.first { it.first == selectedItem }.second

                // Perform your action
                binding.imageRole.setImageDrawable(setImagePicture(mapOfRole.values.first()).drawable)

                if (spinnerId != null) {
                    addToText(spinnerId, selectedItem)
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

        val idList = listOf( binding.soloSpinner.id, binding.nocturneSpinner.id, binding.werewolfSpinner.id
            , binding.categoryEdit.id, binding.powerSpinner.id, binding.conditionalSpinner.id, binding.actionEdit.id
            , binding.descriptionEdit.id)

        mapValues = idList.associateWith { "" }.toMutableMap()

        //List of editText
        val listEdit = listOf(
            binding.categoryEdit
            ,binding.actionEdit
            ,binding.descriptionEdit
        )

        //In creation, we can safely set the edittext
        if(!editionMode) {
            //Affectation
            listEdit.forEach { editText ->
                setEditText(editText, "")
            }
        }


        val listSimpleSpinner = listOf(
            binding.soloSpinner,
            binding.werewolfSpinner,
            binding.nocturneSpinner
        )

        listSimpleSpinner.forEach { spinner ->
            spinner.onItemSelectedListener = createSharedItemSelectedListener()
        }

        val listComplexSpinner = listOf(
            binding.powerSpinner,
            binding.conditionalSpinner
        )

        listComplexSpinner.forEach { spinner ->
            spinner.onItemSelectedListener = createPairSharedItemSelectedListener()
        }

        val listYN = listOf("", "YES", "NO")

        binding.soloSpinner.adapter = SpinnerAdapter(this, listYN)

        binding.werewolfSpinner.adapter = SpinnerAdapter(this, listYN)

        binding.nocturneSpinner.adapter = SpinnerAdapter(this, listYN)

        binding.powerSpinner.adapter = SpinnerPairAdapter(this, PowerState.entries.map { x -> Pair(x.name, x.value)})

        binding.conditionalSpinner.adapter = SpinnerPairAdapter(this, ConditionalActivation.entries.map { x -> Pair(x.name, x.value)})
        binding.conditionalSpinner.isEnabled = false
        binding.conditionalSpinner.alpha = 0.5f

        val arrayCheck = BooleanArray(listLink.size) { false }

        binding.linkButton.setOnClickListener {

            // Show the dialog when spinner is clicked
            AlertDialog.Builder(this)
                .setTitle("Choose Options")
                .setMultiChoiceItems(
                    listLink.toTypedArray()
                    , arrayCheck) { _, index, isChecked ->
                    arrayCheck[index] = isChecked
                    if(isChecked)
                        rolesForLink.add(listLink[index])
                    else
                        rolesForLink.remove(listLink[index])
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.validButton.setOnClickListener {
            createOrUpdateNewRole()
        }

        binding.returnButton.setOnClickListener {
            finish()
        }

        if(editionMode) {
            val pairPosition = listRoles.map { it.first }.indexOf(characterEdit!!.className)
            binding.roleSpinner.setSelection(pairPosition)
            binding.roleSpinner.isEnabled = false
            binding.roleSpinner.alpha = 0.5f

            setPositionOfSpinner(binding.soloSpinner, listYN, if(characterEdit!!.isSolo) "YES" else "NO")
            setPositionOfSpinner(binding.werewolfSpinner, listYN, if(characterEdit!!.isWerewolf) "YES" else "NO")
            setPositionOfSpinner(binding.nocturneSpinner, listYN, if(characterEdit!!.isNocturnal) "YES" else "NO")
            setPositionOfSpinner(binding.powerSpinner, PowerState.entries.map { it.value}, characterEdit!!.powerState.value)
            setPositionOfSpinner(binding.conditionalSpinner, ConditionalActivation.entries.map { it.value}, characterEdit!!.condition.value)

            rolesForLink = characterEdit!!.rolesToStick.toMutableList()

            listEdit.forEach { editText ->

                //Important to set up after the text in order to avoid
                //Infinite loop
                when(editText.id) {
                    binding.categoryEdit.id -> setEditText(editText, characterEdit!!.mode)
                    binding.actionEdit.id -> setEditText(editText, characterEdit!!.action)
                    binding.descriptionEdit.id -> setEditText(editText, characterEdit!!.description)
                }
            }


            listLink.forEachIndexed { index, element ->
                if(characterEdit!!.rolesToStick.contains(element)){
                    arrayCheck[index] = true
                }
            }
        }

    }

    private fun setPositionOfSpinner(spinner : Spinner, list: List<String>, data : String) {
        val position = list.indexOf(data)
        if(position > -1) {
            spinner.setSelection(position)
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

    private fun createOrUpdateNewRole() {

        //In edition mode, we delete the previous one
        if(editionMode)
            WorkerEasier.deleteCharacterFromList(characterEdit!!)

        val descriptionRole = mapValues[binding.descriptionEdit.id]!!
        val actionRole = mapValues[binding.actionEdit.id]!!
        val isSoloRole = mapValues[binding.soloSpinner.id] == "YES"
        val nocturneRole = mapValues[binding.nocturneSpinner.id] == "YES"
        val isWerewolfRole = mapValues[binding.werewolfSpinner.id] == "YES"
        val powerRole = PowerState.valueOf(mapValues[binding.powerSpinner.id]!!)
        val conditionRole = ConditionalActivation.valueOf(mapValues[binding.conditionalSpinner.id]!!)
        val stickRole = rolesForLink.toTypedArray()
        val baseRole = mapValues[binding.roleSpinner.id]!!
        val keyTypeRole = mapValues[binding.categoryEdit.id]!!.uppercase().trim()

        val maxOccurrenceRole = WorkerEasier.characterListType.first { it.first == baseRole }.second.values.first().maxOccurrence

        val newCharacterGame = CharacterGame(
            descriptionRole,
            actionRole,
            isSoloRole,
            nocturneRole,
            powerRole,
            conditionRole,
            isWerewolfRole,
            stickRole,
            0,
            maxOccurrenceRole,
            keyTypeRole
        )

        val newCharacter = WorkerEasier.getGoodCharacterCast(newCharacterGame, baseRole)

        WorkerEasier.addOrUpdateNewCharacter(newCharacter, baseRole, keyTypeRole)

        finish()
    }

    private fun setEditText(editText: EditText, text: String) {

        editText.setText(text)
        mapValues[editText.id] = text

        editText.addTextChangedListener(createSharedTextWatcher())
        editText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                true
            } else false
        }

        editText.imeOptions = EditorInfo.IME_ACTION_NONE

        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
    }

    private fun addToText(key: Int, value: String){
        mapValues[key] = value

        val keyCodeForbid = listOf("NORMAL", "AUTHOR").contains(binding.categoryEdit.text.toString())

        val hasEmpty = mapValues.values.any { it == "" }

        binding.validButton.isEnabled = !hasEmpty && !keyCodeForbid
    }

}