package main.werewolfkotlin

import Model.*
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
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import com.google.gson.Gson
import main.werewolfkotlin.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityMainBinding

    //List of Characters from the selection
    private val charactersMenu : MutableList<Character> = mutableListOf()

    //List of selected characters for the game
    private val charactersSelected : MutableList<Character> = mutableListOf()

    //Dimensions for image
    private val imageDimension : Int = 240
    private val imageMargin : Int = 10

    //Selection for actions in list
    private var selectedCharacterInGame: Character? = null

    //Chart used to enable/disable addition of some characters
    private var characterChart : MutableList<CharacterChart> = mutableListOf()

    //Limit of characters to start playing.(See if can be configured)
    private val limitStartNumber : Int = 5

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //All roles will be hard coded for the chart
        val villager = Villager(1)
        val werewolf = Werewolf(2)
        val seer = Seer(3)
        val witch = Witch(4)
        val hunter = Hunter(5)
        val littleGirl = LittleGirl(6)
        val thief = Thief(7)
        val cupid = Cupid(8)

        //Adding to the list hard coded
        charactersMenu.add(villager)
        charactersMenu.add(werewolf)
        charactersMenu.add(seer)
        charactersMenu.add(witch)
        charactersMenu.add(hunter)
        charactersMenu.add(littleGirl)
        charactersMenu.add(thief)
        charactersMenu.add(cupid)

        //Set a picture with an action listener on it
        for ((index, character) in charactersMenu.withIndex()) {

            //Set the imageView
            val imageView : ImageView = setImagePicture(character)

            //Update the chart
            characterChart.add(CharacterChart(character.className,
                0, character.maxOccurence, imageView))

            //Special cases
            //Ex : the simple Thief => doesn't have any interest in this application
            if(character.maxOccurence == 0) {
                imageView.isEnabled = false
                imageView.alpha = 0.5f
            } else {
                //Set the listener. Not active if image is disabled
                imageView.setOnClickListener {
                    //Add character to selection
                    addCharacterToGame(character)

                    //Update view
                    val charChart = characterChart.first{ x -> x.name == character.className}
                    charChart.occurrence += 1

                    //Change image and disable when reach maximal
                    if(charChart.occurrence == charChart.maxOccurrence) {
                        imageView.isEnabled = false
                        imageView.alpha = 0.5f
                    }

                    //Always refresh the game list
                    refreshSelectedListCharacters()
                }
            }

            //Adding to the menu list
            binding.gridSelectView.addView(imageView, index)

        }

        //Adding listeners to the buttons for game
        setButtonListenersOfSelected()

        //Disable by default buttons for list selection
        setSelectionButtonEnable(false)

        //Need an amount of characters to start
        binding.startButton.isEnabled = false

        //Setting button text here
        binding.leftArrowButton.text = "<"
        binding.rightArrowButton.text = ">"

    }

    ///
    /// Execution on restart of the activity
    ///
    override fun onRestart() {
        super.onRestart()
        recreate() // This will restart the activity
    }

    ///
    /// This function apply a picture with an image view
    ///
    private fun setImagePicture(character : Character): ImageView {
        //Set the resource and image
        val imageView = ImageView(this).apply {
            @DrawableRes val img = ImageGetter.getCharacterImage(character)
            setImageResource(img)
            adjustViewBounds = true
        }

        //Set some parameters like the borders
        val layoutParams = LinearLayout.LayoutParams(imageDimension,imageDimension)
        layoutParams.setMargins(0, 0, imageMargin, imageMargin)
        imageView.layoutParams = layoutParams

        return imageView
    }

    ///
    /// Enable or disable all the buttons handling the selected characters
    ///
    private fun setSelectionButtonEnable(enable : Boolean) {
        binding.leftArrowButton.isEnabled = enable
        binding.rightArrowButton.isEnabled = enable
        binding.cancelButton.isEnabled = enable
        binding.removeButton.isEnabled = enable
    }

    ///
    /// Drawing function. Apply some scare selection
    ///
    private fun drawSelection(character : Character) {

        for(layout in binding.gridSelectedView) {
            if(layout.tag == character.className) {
                layout.setPadding(10, 10, 10, 10)
                layout.setBackgroundColor(Color.GREEN)
            } else {
                layout.setPadding(0, 0, 0, 0)
                layout.setBackgroundColor(Color.TRANSPARENT)
            }
        }

    }

    ///
    /// Refresh of the in game list on selection
    ///
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
                text = "x${charactersSelected.count { x -> x.className == character.className}}"

                //Size of the text
                textSize = 14f

                //Color of the text
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))

                //Background of the badge
                background = ContextCompat.getDrawable(this@MainActivity, R.color.white)

                // Add padding
                setPadding(4, 4, 4, 4)
            }

            //Add badge to the layout
            frameLayout.addView(countTextView)

            //Listener of the picture
            imageView.setOnClickListener {
                //Instead of playing on the object itself, we use the name
                if(selectedCharacterInGame?.className != character.className) {

                    //Active character in selection
                    selectedCharacterInGame = character

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

            //Adding layout to the gridlayout
            binding.gridSelectedView.addView(frameLayout, index)

            //Keep selection on draw
            if(selectedCharacterInGame?.className == character.className) {
                drawSelection(character)
            }
        }

        //Refresh again (to see if useless)
        refreshArrowSelectionSelected()

        //Each drawing can update the start. A game start with at least five characters
        binding.startButton.isEnabled = charactersSelected.size >= limitStartNumber
    }

    ///
    /// Add a character to the game
    ///
    private fun addCharacterToGame(character: Character) {
        //Copy the object
        val newCharacter : Character = character.clone()

        //Check if there is a special case of existing characters
        val existingCharacter : Character?
            = if(character is LittleGirl || character is Werewolf) {
                //Case of werewolf and little girl, they must play at the same time
                charactersSelected.firstOrNull{ x -> x.className == "LittleGirl"
                                                    || x.className == "Werewolf" }
        } else {
            //Case same kind of character exist
            charactersSelected.firstOrNull{ x -> x.className == character.className }
        }

        //Special case
        if(existingCharacter != null)
            newCharacter.order = existingCharacter.order //Same order
        else
            newCharacter.order = charactersSelected.size //At the end

        //Add the character to the list correctly
        charactersSelected.add(newCharacter)
    }

    ///
    /// Remove the selected characters from the in game list
    /// A kind of character with multiple occurrence will just reduce the occurrence by one
    ///
    private fun removeCharacterToSelection() {

        //Protection checking selected character is null
        if(selectedCharacterInGame != null) {

            //Character removed
            charactersSelected.remove(selectedCharacterInGame)

            //Update the chart to select again
            val charChart = characterChart.first{ x -> x.name == selectedCharacterInGame!!.className}
            charChart.occurrence -= 1

            //enable the chart and so the listener
            if(charChart.occurrence < charChart.maxOccurrence) {
                charChart.image.isEnabled = true
                charChart.image.alpha = 1f
            }
        }

        //No selected characters
        selectedCharacterInGame = null

        //Disable list
        setSelectionButtonEnable(false)
    }

    ///
    /// Put all the characters at the same order of the selected one
    /// to one previous position
    ///
    private fun previousOrder() {
        //Getting the order of the actual character
        val index : Int = selectedCharacterInGame!!.order

        //Getting all previous characters (case some are in the same moment)
        val previousCharacters = charactersSelected.filter{ x -> x.order == index - 1 }

        //Getting all characters with the order (case some are in the same moment)
        val actualCharacters = charactersSelected.filter{ x -> x.order == index }

        //Upgrade the previous ones
        previousCharacters.forEach {  x -> x.order += 1 }

        //Downgrade the actual ones
        actualCharacters.forEach {  x -> x.order -= 1 }

    }

    ///
    /// Put all the characters at the same order of the selected one
    /// to one next position
    ///
    private fun nextOrder() {
        //Getting the order of the actual character
        val index : Int = selectedCharacterInGame!!.order

        //Getting all previous characters (case some are in the same moment)
        val nextCharacters = charactersSelected.filter{ x -> x.order == index + 1 }

        //Getting all characters with the order (case some are in the same moment)
        val actualCharacters = charactersSelected.filter{ x -> x.order == index }

        //Downgrade the previous ones
        nextCharacters.forEach { x -> x.order -= 1 }

        //Upgrade the actual ones
        actualCharacters.forEach {  x -> x.order += 1 }

    }

    ///
    /// Disable selection of a character in the in game list
    ///
    private fun removeSelectedCharacterInGame() {

        selectedCharacterInGame = null

        //We only refresh view when canceling
        refreshSelectedListCharacters()

    }

    ///
    /// Refresh the availability of the arrows handling the position of in game characters
    ///
    private fun refreshArrowSelectionSelected() {
        if(selectedCharacterInGame != null) {
            binding.leftArrowButton.isEnabled = selectedCharacterInGame!!.order != 0
            binding.rightArrowButton.isEnabled = selectedCharacterInGame!!.order != charactersSelected.maxOf { x -> x.order }
        }

    }

    ///
    /// Init all the buttons of the activity with their listeners
    ///
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