package main.werewolfkotlin

import model.*
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import com.google.gson.Gson
import main.werewolfkotlin.databinding.ActivitySelectBinding
import java.util.Locale

class SelectActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivitySelectBinding

    //List of Characters from the selection
    private val charactersMenus : MutableList<CharacterGame> = mutableListOf()

    //List of selected characters for the game
    private val charactersSelected : MutableList<CharacterGame> = mutableListOf()

    //Dimensions for image
    private val imageDimension : Int = 240
    private val imageMargin : Int = 10

    //Selection for actions in list
    private var selectedCharacterGameInGame: CharacterGame? = null

    //Chart used to enable/disable addition of some characters
    private var characterChart : MutableList<CharacterChart> = mutableListOf()

    //Limit of characters to start playing.(See if can be configured)
    private val limitStartNumber : Int = 8

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Getting the list from the json
        WorkerEasier.characterListType.forEach { pair ->

            //Getting the first character as a base, we don't care
            val characterBase = pair.second.values.first()
            characterBase.order = charactersMenus.size + 1

            charactersMenus.add(characterBase)
        }

        //Set a picture with an action listener on it
        for ((index, character) in charactersMenus.withIndex()) {

            //Set the imageView
            val imageView : ImageView = setImagePicture(character)

            //Update the chart
            characterChart.add(CharacterChart(character.className,
                0, character.maxOccurrence, imageView))

            //Set the listener. Not active if image is disabled
            imageView.setOnClickListener {

                showListDialog(character)

                //Update view
                val charChart = characterChart.first{ x -> x.name == character.className}
                charChart.occurrence += 1

                //Change image and disable when reach maximal
                if(charChart.occurrence == charChart.maxOccurrence) {
                    imageView.isEnabled = false
                    imageView.alpha = 0.5f
                }

            }

            imageView.setOnLongClickListener {

                // Create the dialog
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.InformationView_title))
                    .setMessage(character.description)
                    .setNegativeButton(R.string.Generic_Okay, null)
                    .show()
                true // Return true to indicate the long-click event is consumed
            }


            //Adding to the menu list
            binding.gridSelectView.addView(imageView, index)

        }

        //Adding listeners to the buttons for game
        setButtonListenersOfSelected()

        //Disable by default buttons for list selection
        setSelectionButtonEnable(false)

        //Update string
        updateSelectedCharacterString()

        //Need an amount of characters to start
        binding.startButton.isEnabled = false

        binding.closeButton.setOnClickListener {
            finish()
        }
    }

    /***
     * Show a small dialog where user can select type of character
     */
    private fun showListDialog(characterGame: CharacterGame) {

        //This can't be null and fail
        val characterMap = WorkerEasier.characterListType
            .firstOrNull { x -> x.first == characterGame.className }!!.second

        // List of items
        val items = characterMap.keys.toMutableList()

        //Unique version => direct add
        if(items.size == 1) {

            //Getting the character at its unique version
            val characterSelected = WorkerEasier.characterListType
                .firstOrNull { x -> x.first == characterGame.className }!!
                .second[items[0]]!!

            //Add character to selection
            addCharacterToGame(characterSelected)

            //Always refresh the game list
            refreshSelectedListCharacters()

        } else {

            // Create the dialog
            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.SelectView_selectionTitle))
                .setItems(items.toTypedArray()) { _, which ->
                    // Handle item selection
                    val selectedItem = items[which]
                    Toast.makeText(this, String.format(getString(R.string.SelectView_selectionMessage), characterGame.name, selectedItem), Toast.LENGTH_SHORT).show()

                    //Get the character at the selected version
                    val characterSelected = WorkerEasier.characterListType
                        .firstOrNull { x -> x.first == characterGame.className }!!
                        .second[selectedItem]!!

                    //Add character to selection
                    addCharacterToGame(characterSelected)

                    //Always refresh the game list
                    refreshSelectedListCharacters()

                }
                .setNegativeButton("Cancel") { _, _ -> cancelClicking(characterGame) } //Some users prefer a cancel button
                .show()

            //Listener on cancel by taping outside of the dialog
            dialog.setOnCancelListener {
                cancelClicking(characterGame)
            }
        }

    }

    /***
     * Action on canceling the dialog. Refresh the occurrence of the character in the Chart
     */
    private fun cancelClicking(characterGame : CharacterGame) {
        //Update the chart to select again
        val charChart = characterChart.first{ x -> x.name == characterGame.className}
        charChart.occurrence -= 1

        //enable the chart and so the listener
        if(charChart.occurrence < charChart.maxOccurrence) {
            charChart.image.isEnabled = true
            charChart.image.alpha = 1f
        }
    }

    /***
     * Execution on restart of the activity
     */
    override fun onRestart() {
        super.onRestart()
        recreate() // This will restart the activity
    }

    /***
     * Dynamic update of the string info on number of characters
     */
    private fun updateSelectedCharacterString() {
        val stringResource = getString(R.string.SelectView_NumberOfCharacters)

        //Set message
        binding.textMenu.text = String.format(stringResource, charactersSelected.size)
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

        //Set some parameters like the borders
        val layoutParams = LinearLayout.LayoutParams(imageDimension,imageDimension)
        layoutParams.setMargins(0, 0, imageMargin, imageMargin)
        imageView.layoutParams = layoutParams

        return imageView
    }

    /***
     * Enable or disable all the buttons handling the selected characters
     */
    private fun setSelectionButtonEnable(enable : Boolean) {
        binding.leftArrowButton.isEnabled = enable
        binding.rightArrowButton.isEnabled = enable
        binding.cancelButton.isEnabled = enable
        binding.removeButton.isEnabled = enable
    }

    /***
     * Drawing function. Apply some scare selection
     */
    private fun drawSelection(characterGame : CharacterGame) {

        for(layout in binding.gridSelectedView) {
            if(layout.tag == characterGame.className) {
                layout.setPadding(5, 5, 0, 0)
                layout.setBackgroundColor(Color.GREEN)
            } else {
                layout.setPadding(0, 0, 0, 0)
                layout.setBackgroundColor(Color.TRANSPARENT)
            }
        }

    }

    /***
     * Refresh of the in game list on selection
     */
    private fun refreshSelectedListCharacters() {

        //Always remove to recreate
        binding.gridSelectedView.removeAllViewsInLayout()

        //In case, sort by order
        charactersSelected.sortBy{ x -> x.order }

        for ((index, character) in charactersSelected.distinctBy{ x -> x.className}.withIndex()) {

            // Create a FrameLayout to hold ImageView and TextView
            val frameLayout = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }
                //Apply a tag for targeting in selection
                tag = character.className
            }

            //Create imageView
            val imageView : ImageView = setImagePicture(character)

            //Add imageView to the layout
            frameLayout.addView(imageView)

            //Adding a badge which has the count of characters of same kind
            val countTextView = TextView(this).apply {

                //Parameters like wrapping, gravity, margins
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, 4, 4) // Position in bottom-right corner
                }

                //Name of the text
                text = String.format("x%s",
                    charactersSelected.count { x -> x.className == character.className}.toString()
                )

                //Size of the text
                textSize = 14f

                //Color of the text
                setTextColor(ContextCompat.getColor(this@SelectActivity, android.R.color.black))

                //Background of the badge
                background = ContextCompat.getDrawable(this@SelectActivity, R.color.white)

                // Add padding
                setPadding(4, 4, 4, 4)
            }

            //Add badge to the layout
            frameLayout.addView(countTextView)

            //Listener of the picture
            imageView.setOnClickListener {
                //Instead of playing on the object itself, we use the name
                if(selectedCharacterGameInGame?.className != character.className) {

                    //Active character in selection
                    selectedCharacterGameInGame = character

                    //Activate all the button to handle the in game list
                    setSelectionButtonEnable(true)

                    //Put selection in evidence
                    drawSelection(character)
                } else { //Already selected = not selected anymore

                    //No selection, disable all handling buttons
                    setSelectionButtonEnable(false)

                    //Remove from selection
                    removeSelectedCharacterInGame()
                }

                //Always refresh arrow selection (to see if misplaced)
                refreshArrowSelectionSelected()
            }

            imageView.setOnLongClickListener {

                // Create the dialog
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.InformationView_title))
                    .setMessage(character.description)
                    .setNegativeButton(getString(R.string.Generic_Okay), null)
                    .show()
                true // Return true to indicate the long-click event is consumed
            }

            //Adding layout to the gridlayout
            binding.gridSelectedView.addView(frameLayout, index)

            //Keep selection on draw
            if(selectedCharacterGameInGame?.className == character.className) {
                drawSelection(character)
            }
        }

        //Refresh again (to see if useless)
        refreshArrowSelectionSelected()

        //Each drawing can update the start. A game start with at least five characters
        binding.startButton.isEnabled = charactersSelected.size >= limitStartNumber

        //Update string
        updateSelectedCharacterString()
    }

    /***
     * Add a character to the game
     */
    private fun addCharacterToGame(characterGame: CharacterGame) {

        //Copy the object
        val newCharacterGame : CharacterGame = characterGame.clone()

        //Check if there is a special case of existing characters
        val existingCharacterGame : CharacterGame?
            = if(!charactersSelected.any { it.className == characterGame.className }
                    && characterGame.rolesToStick.isNotEmpty()) {
                //Each character has a link order configured
                charactersSelected.firstOrNull{ x -> characterGame.rolesToStick.contains(
                    x.className.plus("-").plus(x.mode).uppercase(Locale.ROOT))}
        } else {
            //Case same kind of character exist
            charactersSelected.firstOrNull{ x -> x.className == characterGame.className }
        }

        //Special case
        if(existingCharacterGame != null)
            newCharacterGame.order = existingCharacterGame.order //Same order
        else {
            newCharacterGame.order = if(charactersSelected.isEmpty())  0
                                else charactersSelected.maxOf { x -> x.order } + 1 //At the end
        }


        //Add the character to the list correctly
        charactersSelected.add(newCharacterGame)
    }

    /***
     * Remove the selected characters from the in game list
     * A kind of character with multiple occurrence will just reduce the occurrence by one
     */
    private fun removeCharacterToSelection() {

        //Protection checking selected character is null
        if(selectedCharacterGameInGame != null) {

            //Character removed
            charactersSelected.remove(selectedCharacterGameInGame)

            //Update the chart to select again
            cancelClicking(selectedCharacterGameInGame!!)
        }

        //No selected characters
        selectedCharacterGameInGame = null

        //Disable list
        setSelectionButtonEnable(false)
    }

    /***
     * Put all the characters at the same order of the selected one
     * to one previous position
     */
    private fun previousOrder() {
        //Getting the order of the actual character
        val index : Int = selectedCharacterGameInGame!!.order

        //Getting all previous characters (case some are in the same moment)
        val previousCharacters = charactersSelected.filter{ x -> x.order == index - 1 }

        //Getting all characters with the order (case some are in the same moment)
        val actualCharacters = charactersSelected.filter{ x -> x.order == index }

        //Upgrade the previous ones
        previousCharacters.forEach {  x -> x.order += 1 }

        //Downgrade the actual ones
        actualCharacters.forEach {  x -> x.order -= 1 }

    }

    /***
     * Put all the characters at the same order of the selected one
     * to one next position
     */
    private fun nextOrder() {
        //Getting the order of the actual character
        val index : Int = selectedCharacterGameInGame!!.order

        //Getting all previous characters (case some are in the same moment)
        val nextCharacters = charactersSelected.filter{ x -> x.order == index + 1 }

        //Getting all characters with the order (case some are in the same moment)
        val actualCharacters = charactersSelected.filter{ x -> x.order == index }

        //Downgrade the previous ones
        nextCharacters.forEach { x -> x.order -= 1 }

        //Upgrade the actual ones
        actualCharacters.forEach {  x -> x.order += 1 }

    }

    /***
     * Disable selection of a character in the in game list
     */
    private fun removeSelectedCharacterInGame() {

        selectedCharacterGameInGame = null

        //We only refresh view when canceling
        refreshSelectedListCharacters()

        setSelectionButtonEnable(false)
    }

    /***
     * Refresh the availability of the arrows handling the position of in game characters
     */
    private fun refreshArrowSelectionSelected() {
        if(selectedCharacterGameInGame != null) {
            binding.leftArrowButton.isEnabled = selectedCharacterGameInGame!!.order != 0
            binding.rightArrowButton.isEnabled = selectedCharacterGameInGame!!.order != charactersSelected.maxOf { x -> x.order }
        }

    }

    /***
     * Init all the buttons of the activity with their listeners
     */
    private fun setButtonListenersOfSelected() {

        //Starting button
        binding.startButton.setOnClickListener {

            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, GameActivity::class.java)

            //Create the gson
            val gson = Gson()

            try {
                //Json the list
                val strList = gson.toJson(charactersSelected)

                //Insert it in a kind of "cache"
                intent.putExtra("GameList", strList)

                //Start the other activity
                startActivity(intent)

            } catch(ex : Exception) {
                println(ex)
            }

        }

        //Left arrow button listener
        binding.leftArrowButton.setOnClickListener {
            previousOrder()
            refreshSelectedListCharacters()
        }

        //Right arrow button listener
        binding.rightArrowButton.setOnClickListener {
            nextOrder()
            refreshSelectedListCharacters()
        }

        //Cancel button listener
        binding.cancelButton.setOnClickListener {
            removeSelectedCharacterInGame()
        }

        //Remove button listener
        binding.removeButton.setOnClickListener {
            removeCharacterToSelection()
            refreshSelectedListCharacters()
        }
    }
}