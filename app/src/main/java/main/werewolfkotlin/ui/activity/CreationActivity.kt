package main.werewolfkotlin.ui.activity

import main.werewolfkotlin.R
import main.werewolfkotlin.data.RolesRepository
import main.werewolfkotlin.data.stringByKey
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.adapter.SpinnerAdapter
import main.werewolfkotlin.ui.adapter.SpinnerPairAdapter
import main.werewolfkotlin.ui.design.RoleImages
import model.stickKey

import android.content.Context
import android.graphics.Color
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
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityCreationBinding
import model.CharacterGame
import model.ConditionalActivation
import model.PowerState

class CreationActivity : AppCompatActivity() {

    companion object {
        /** Name of the role to change ("Seer"...). Absent : a new role is being created */
        const val EXTRA_ROLE_NAME = "RoleName"

        /** Mode of the role to change ("NORMAL", "AUTHOR"...) */
        const val EXTRA_ROLE_MODE = "RoleMode"
    }

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityCreationBinding

    //Getting all the inputs associated to the fitting id
    private lateinit var mapValues : MutableMap<Int, String>

    //Linked roles
    private var rolesForLink : MutableList<String> = mutableListOf()

    //Actual character in edition mode
    private var characterEdit : CharacterGame? = null

    //Map for all the help text
    private lateinit var mapHelper : MutableMap<Int, String>

    //Getting the yes confirmation in its language
    private lateinit var yes : String

    //Getting the no negation in its language
    private lateinit var no : String

    /***
     * Shared text watcher between all textEdit
     */
    private fun createSharedTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                // Get the current EditText using binding's focused child (or track focus manually)
                val currentEditText = currentFocus as? EditText

                if(currentEditText != null)
                    addToText(currentEditText.id, currentEditText.text.toString())
            }
        }
    }

    /***
     * Spinner adapter on pair to print the correct associate language but keep the key
     */

    /***
     * Shared listener on listener of spinners with pair adapter
     */
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
                            binding.powerSpinner.selectedItem == Labels.power(this@CreationActivity, PowerState.CONDITIONAL)
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

    /***
     * Spinner adapter simple
     */

    /***
     * Spinner simple listener
     */
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

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        yes = getString(R.string.Generic_Yes)
        no = getString(R.string.Generic_No)

        characterEdit = roleBeingChanged()

        //Every role of the roles file : its name for the spinner, and its links for the link button
        val listRoles = RolesRepository.roleTypes.map { Pair(it.first, it.second.values.first().name) }
        val listLink = RolesRepository.roleTypes.flatMap { role ->
            role.second.keys.map { mode -> stickKey(role.first, mode) }
        }

        //The three fields the moderator types in
        val listEdit = listOf(binding.categoryEdit, binding.actionEdit, binding.descriptionEdit)
        val listYN = listOf("", yes, no)
        val arrayCheck = BooleanArray(listLink.size) { false }

        prepareValues()
        prepareRoleSpinner(listRoles)
        prepareSpinners(listYN)
        prepareEditTexts(listEdit)
        bindButtons(listLink, arrayCheck)

        //Edition mode : the form starts with the values of the role being changed
        fillFormWith(characterEdit ?: return, listRoles, listYN, listEdit, listLink, arrayCheck)
    }

    /***
     * The role the description screen asked to change, or null when a role is being created.
     *
     * The description screen only gives the name and the mode of the role : it is read from the
     * roles file, so there is never a copy of a role travelling between two screens.
     * A copy is taken, so that changing the form never touches the role of the roles file.
     */
    private fun roleBeingChanged(): CharacterGame? {
        val roleName = intent.getStringExtra(EXTRA_ROLE_NAME) ?: return null
        val roleMode = intent.getStringExtra(EXTRA_ROLE_MODE) ?: return null
        return RolesRepository.prototypeOf(roleName, roleMode)?.clone()
    }

    /***
     * One place per field of the form : what the moderator chose, and the tip of the field
     */
    private fun prepareValues() {
        val fields = listOf(
            binding.soloSpinner, binding.nocturneSpinner, binding.werewolfSpinner,
            binding.categoryEdit, binding.powerSpinner, binding.conditionalSpinner,
            binding.actionEdit, binding.descriptionEdit
        )

        val tips = listOf(
            binding.soloText, binding.nocturneText, binding.werewolfText,
            binding.categoryText, binding.powerText, binding.conditionalText,
            binding.actionText, binding.descriptionText
        )

        mapValues = fields.map { it.id }.associateWith { "" }.toMutableMap()
        mapHelper = mapValues.toMutableMap()
        setMapHelper(tips)
    }

    /***
     * The spinner of the role : its picture follows the chosen role
     */
    private fun prepareRoleSpinner(listRoles: List<Pair<String, String>>) {
        val pairAdapter = SpinnerPairAdapter(this, listRoles)
        binding.roleSpinner.adapter = pairAdapter

        binding.roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = pairAdapter.getValue(position)

                //Picture of the chosen role
                val variants = RolesRepository.roleTypes.firstOrNull { it.first == selectedItem }?.second
                if (variants != null) binding.imageRole.setImageResource(RoleImages.of(variants.values.first()))

                parent?.id?.let { addToText(it, selectedItem) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing to do : the form keeps the value it had
            }
        }
    }

    /***
     * The spinners of the characteristics : yes / no, power, and condition of the power
     */
    private fun prepareSpinners(listYN: List<String>) {
        listOf(binding.soloSpinner, binding.werewolfSpinner, binding.nocturneSpinner).forEach { spinner ->
            spinner.adapter = SpinnerAdapter(this, listYN)
            spinner.onItemSelectedListener = createSharedItemSelectedListener()
        }

        listOf(binding.powerSpinner, binding.conditionalSpinner).forEach { spinner ->
            spinner.onItemSelectedListener = createPairSharedItemSelectedListener()
        }

        binding.powerSpinner.adapter =
            SpinnerPairAdapter(this, PowerState.entries.map { Pair(it.name, Labels.power(this, it)) })

        binding.conditionalSpinner.adapter =
            SpinnerPairAdapter(this, ConditionalActivation.entries.map { Pair(it.name, Labels.condition(this, it)) })

        //The condition only matters for a conditional power
        binding.conditionalSpinner.isEnabled = false
        binding.conditionalSpinner.alpha = 0.5f
    }

    /***
     * In creation, the typed fields start empty. In edition they are filled afterwards
     */
    private fun prepareEditTexts(listEdit: List<EditText>) {
        if (characterEdit != null) return
        listEdit.forEach { editText -> setEditText(editText, "") }
    }

    /***
     * The buttons of the screen : the links between roles, saving, and going back
     */
    private fun bindButtons(listLink: List<String>, arrayCheck: BooleanArray) {
        binding.linkButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.CreationView_Linked)
                .setMultiChoiceItems(listLink.toTypedArray(), arrayCheck) { _, index, isChecked ->
                    arrayCheck[index] = isChecked
                    if (isChecked) rolesForLink.add(listLink[index]) else rolesForLink.remove(listLink[index])
                }
                .setNegativeButton(getString(R.string.Generic_Cancel), null)
                .show()
        }

        binding.validButton.setOnClickListener { createOrUpdateNewRole() }
        binding.returnButton.setOnClickListener { finish() }
    }

    /***
     * Value typed or chosen in the form for this field. Empty when the moderator did not fill it
     */
    private fun formValue(viewId: Int): String = mapValues[viewId] ?: ""

    /***
     * Set the position of the spinner in parameter
     */
    private fun setPositionOfSpinner(spinner: Spinner, list: List<String>, data: String) {
        val position = list.indexOf(data)
        if (position > -1) spinner.setSelection(position)
    }

    /***
     * Put the values of a role in the form. The role itself cannot be changed : only its values
     */
    private fun fillFormWith(
        role: CharacterGame,
        listRoles: List<Pair<String, String>>,
        listYN: List<String>,
        listEdit: List<EditText>,
        listLink: List<String>,
        arrayCheck: BooleanArray
    ) {
        //The role of the form is fixed : only its values can be changed
        binding.roleSpinner.setSelection(listRoles.map { it.first }.indexOf(role.className))
        binding.roleSpinner.isEnabled = false
        binding.roleSpinner.alpha = 0.5f

        setPositionOfSpinner(binding.soloSpinner, listYN, if (role.isSolo) yes else no)
        setPositionOfSpinner(binding.werewolfSpinner, listYN, if (role.isWerewolf) yes else no)
        setPositionOfSpinner(binding.nocturneSpinner, listYN, if (role.isNocturnal) yes else no)
        setPositionOfSpinner(
            binding.powerSpinner,
            PowerState.entries.map { Labels.power(this, it) },
            Labels.power(this, role.powerState)
        )
        setPositionOfSpinner(
            binding.conditionalSpinner,
            ConditionalActivation.entries.map { Labels.condition(this, it) },
            Labels.condition(this, role.condition)
        )

        rolesForLink = role.rolesToStick.toMutableList()

        //Important to set up after the text in order to avoid an infinite loop
        listEdit.forEach { editText ->
            when (editText.id) {
                binding.categoryEdit.id -> setEditText(editText, role.mode)
                binding.actionEdit.id -> setEditText(editText, role.action)
                binding.descriptionEdit.id -> setEditText(editText, role.description)
            }
        }

        listLink.forEachIndexed { index, element ->
            if (role.rolesToStick.contains(element)) arrayCheck[index] = true
        }
    }

    /***
     * Create the new role. If it was an edition, delete the previous one and create the new one.
     * This can allow a change of mode
     */
    private fun createOrUpdateNewRole() {

        //In edition mode, we delete the previous one
        characterEdit?.let { RolesRepository.deleteCharacterFromList(it) }

        val descriptionRole = formValue(binding.descriptionEdit.id)
        val actionRole = formValue(binding.actionEdit.id)
        val isSoloRole = formValue(binding.soloSpinner.id) == yes
        val nocturneRole = formValue(binding.nocturneSpinner.id) == yes
        val isWerewolfRole = formValue(binding.werewolfSpinner.id) == yes
        val powerRole = PowerState.entries.firstOrNull { it.name == formValue(binding.powerSpinner.id) }
            ?: PowerState.PERMANENT
        val conditionRole = ConditionalActivation.entries.firstOrNull { it.name == formValue(binding.conditionalSpinner.id) }
            ?: ConditionalActivation.NO_CONDITION
        val stickRole = rolesForLink.toTypedArray()
        val baseRole = formValue(binding.roleSpinner.id)
        val keyTypeRole = formValue(binding.categoryEdit.id).uppercase().trim()

        //A role of the game is always based on an existing role : without it, nothing can be saved
        val baseVariants = RolesRepository.roleTypes.firstOrNull { it.first == baseRole }?.second
        if (baseVariants == null || keyTypeRole.isEmpty()) {
            Toast.makeText(this, R.string.CreationView_Incomplete, Toast.LENGTH_SHORT).show()
            return
        }
        val maxOccurrenceRole = baseVariants.values.first().maxOccurrence

        val newCharacter = CharacterGame(
            baseRole,
            stringByKey(baseRole),
            descriptionRole,
            actionRole,
            powerRole,
            nocturneRole,
            isWerewolfRole,
            isSoloRole,
            maxOccurrenceRole,
            conditionRole,
            stickRole,
            characterEdit?.doTargeting ?: true,
            characterEdit?.doKilling ?: false,
            keyTypeRole
        )

        RolesRepository.addOrUpdateNewCharacter(newCharacter, baseRole, keyTypeRole)

        finish()
    }

    /***
     * Setting the map helper for tips on each part of the creation
     */
    private fun setMapHelper(list : List<TextView>) {
        list.forEach { view ->
            when(view.id) {
                binding.soloText.id -> mapHelper[view.id] = getString(R.string.CreationView_SoloText)
                binding.nocturneText.id -> mapHelper[view.id] = getString(R.string.CreationView_NocturneText)
                binding.werewolfText.id -> mapHelper[view.id] = getString(R.string.CreationView_WerewolfText)
                binding.categoryText.id -> mapHelper[view.id] = getString(R.string.CreationView_ModeText)
                binding.powerText.id -> mapHelper[view.id] = getString(R.string.CreationView_PowerText)
                binding.conditionalText.id -> mapHelper[view.id] = getString(R.string.CreationView_ConditionText)
                binding.actionText.id -> mapHelper[view.id] = getString(R.string.CreationView_ActionText)
                binding.descriptionText.id -> mapHelper[view.id] = getString(R.string.CreationView_DescriptionText)
            }

            view.setOnClickListener {

                val message = mapHelper[view.id]

                // Show the help fitting when clicking
                AlertDialog.Builder(this)
                    .setTitle(view.text)
                    .setMessage(message)
                    .setNegativeButton(getString(R.string.Generic_Okay), null)
                    .show()

            }

        }
    }

    /***
     * Set the edit text with a possible text in case of edition
     * Also put the listener on it to keep values for saving
     */
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

    /***
     * Add the info on the associate map and enable/disable the button
     * for the validation of the formula.
     */
    private fun addToText(key: Int, value: String){
        mapValues[key] = value

        val keyCodeForbid = listOf("NORMAL", "AUTHOR").contains(binding.categoryEdit.text.toString())

        val hasEmpty = mapValues.values.any { it == "" }

        binding.validButton.isEnabled = !hasEmpty && !keyCodeForbid
    }

}