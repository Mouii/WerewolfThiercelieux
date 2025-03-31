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
    private val limitStartNumber : Int = 8

    //List of custom characters created by the author
    private val authorList: MutableList<String> = mutableListOf()

    private var strSelection : String = "Number of characters selected : "

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
        val elder = Elder(9)
        val defender = Defender(10)
        val scapegoat = Scapegoat(11)
        val idiot = Idiot(12)
        val piper = Piper(13)
        val actor = Actor(14)
        val angel = Angel(15)
        val bearTamer = BearTamer(16)
        val brother = Brother(17)
        val sister = Sister(18)
        val fox = Fox(19)
        val gypsy = Gypsy(20)
        val rustyKnight = RustyKnight(21)
        val servant = Servant(22)
        val judge = StutteringJudge(23)
        val villagerVillager = VillagerVillager(24)
        val manipulator = Manipulator(25)
        val bigBadWolf = BigBadWolf(26)
        val whiteWerewolf = WhiteWerewolf(27)
        val wildChild = WildChild(28)
        val wolfFather = WolfFather(29)
        val wolfHound = WolfHound(30)
        val crow = Crow(31)
        val fireman = Fireman(32)

        //Adding to the list hard coded
        charactersMenu.add(villager)
        charactersMenu.add(werewolf)
        charactersMenu.add(seer)
        charactersMenu.add(witch)
        charactersMenu.add(hunter)
        charactersMenu.add(littleGirl)
        charactersMenu.add(thief)
        charactersMenu.add(cupid)
        charactersMenu.add(elder)
        charactersMenu.add(defender)
        charactersMenu.add(scapegoat)
        charactersMenu.add(idiot)
        charactersMenu.add(piper)
        charactersMenu.add(actor)
        charactersMenu.add(angel)
        charactersMenu.add(bearTamer)
        charactersMenu.add(brother)
        charactersMenu.add(sister)
        charactersMenu.add(fox)
        charactersMenu.add(gypsy)
        charactersMenu.add(rustyKnight)
        charactersMenu.add(servant)
        charactersMenu.add(judge)
        charactersMenu.add(villagerVillager)
        charactersMenu.add(manipulator)
        charactersMenu.add(bigBadWolf)
        charactersMenu.add(whiteWerewolf)
        charactersMenu.add(wildChild)
        charactersMenu.add(wolfFather)
        charactersMenu.add(wolfHound)
        charactersMenu.add(crow)
        charactersMenu.add(fireman)

        //Adding special class of the author
        authorList.add(littleGirl.className)
        authorList.add(thief.className)
        authorList.add(cupid.className)
        authorList.add(scapegoat.className)
        authorList.add(actor.className)
        authorList.add(angel.className)
        authorList.add(rustyKnight.className)
        authorList.add(servant.className)
        authorList.add(judge.className)
        authorList.add(manipulator.className)
        authorList.add(bigBadWolf.className)
        authorList.add(wolfHound.className)

        //Set a picture with an action listener on it
        for ((index, character) in charactersMenu.withIndex()) {

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
                    .setTitle("Description")
                    .setMessage(character.description)
                    .setNegativeButton("Okay", null)
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

        //Set message
        binding.textMenu.text = strSelection + charactersSelected.size

        //Need an amount of characters to start
        binding.startButton.isEnabled = false

        //Setting button text here
        binding.leftArrowButton.text = "<"
        binding.rightArrowButton.text = ">"

        //Setting background image
        binding.root.setBackgroundResource(R.drawable.werewolf_menu)
        binding.root.background.alpha = 128

    }

    ///
    /// Show a small dialog where user can select type of character
    ///
    private fun showListDialog(character: Character) {
        // List of items
        val items = CharacterMode.entries.toTypedArray().map { x -> x.name}.toMutableList()

        //Unique version => direct add
        if(!authorList.contains(character.className)) {
            //Add character to selection
            addCharacterToGame(character)

            //Always refresh the game list
            refreshSelectedListCharacters()

        } else if (character is Thief) {//Thief is special case for now
            addingCharacterFromAuthorGameMode(character)

            //Add character to selection
            addCharacterToGame(character)

            //Always refresh the game list
            refreshSelectedListCharacters()
        } else {

            // Create the dialog
            val dialog = AlertDialog.Builder(this)
                .setTitle("Select an Item")
                .setItems(items.toTypedArray()) { _, which ->
                    // Handle item selection
                    val selectedItem = items[which]
                    Toast.makeText(this, "Selected Version of ${character.className} : $selectedItem", Toast.LENGTH_SHORT).show()

                    val modeSelected : CharacterMode = CharacterMode.valueOf(selectedItem)

                    //If selected Author mode, change the properties
                    if(modeSelected == CharacterMode.AUTHOR)
                        addingCharacterFromAuthorGameMode(character)

                    //Add character to selection
                    addCharacterToGame(character)

                    //Always refresh the game list
                    refreshSelectedListCharacters()

                }
                .setNegativeButton("Cancel") { _, _ -> cancelClicking(character) } //Some users prefer a cancel button
                .show()

            //Listener on cancel by taping outside of the dialog
            dialog.setOnCancelListener {
                cancelClicking(character)
            }
        }

    }

    ///
    /// Action on canceling the dialog. Refresh the occurrence of the character in the Chart
    ///
    private fun cancelClicking(character : Character) {
        //Update the chart to select again
        val charChart = characterChart.first{ x -> x.name == character.className}
        charChart.occurrence -= 1

        //enable the chart and so the listener
        if(charChart.occurrence < charChart.maxOccurrence) {
            charChart.image.isEnabled = true
            charChart.image.alpha = 1f
        }
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
                layout.setPadding(5, 5, 0, 0)
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

            imageView.setOnLongClickListener {

                // Create the dialog
                AlertDialog.Builder(this)
                    .setTitle("Description")
                    .setMessage(character.description)
                    .setNegativeButton("Okay", null)
                    .show()
                true // Return true to indicate the long-click event is consumed
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

        //Set message
        binding.textMenu.text = strSelection + charactersSelected.size
    }

    ///
    /// Hard coded definition of some characters created by the author
    ///
    private fun addingCharacterFromAuthorGameMode(character: Character) {
        when(character) {
            is LittleGirl -> {
                character.description = "The LittleGirl is a frail child. He/she can't sleep alone and so he/she wakes up to sleep with someone else. He/she choose a character to sleep with. The player wakes up and see the little girl and show his/her role. If it is a werewolf, the little girl is dead. This count as an additional victim."
                character.action = "The LittleGirl choose another character to sleep with. The character wakes up, show his/her role and see the little girl. If it is a werewolf, the LittleGirl die."
                character.isWerewolf = false //not forget, don't spy
            }
            is Thief -> {
                character.description = "The Thief is a master trap. He/she can put traps on other characters and have more information. There is three traps available that can be put each one one time per game : silent one (Thief knows if the selected has done something during the night at the morning), noisy one (everybody knows when the role is called), injure one (everybody knows who did something during the night at the morning). If a trap isn't activated, it is available again. Activation is done when a character do an action, meaning the trapped can wakes up but do nothing and so not activate the trap."
                character.action = "The Thief choose a trap to put between the three ones : silent one, noisy one, injure one. If the trapped doesn't do anything during the night or don't wake up, the trap is available again"
                character.powerState = PowerState.CONSUMABLE
            }
            is Cupid -> {
                character.description = "The Cupid has been touched too much by love. He/She select two people to fall in love together. Then, he/she choose a cheater and a lover. If the lover dies, the three dies. If the cheater dies, the three dies. If the cheated dies, he's the only one. Lover and cheater can't go against each others. Cheated can't only go against the cheater."
                character.action = "The Cupid choose three people to put in a triangle of love. Select two people to make in love, then a cheater and a lover. If the lover dies, the three dies. If the cheater dies, the three dies. If the cheated dies, he's the only one. Lover and cheater can't go against each others. Cheated can't only go against the cheater."
            }
            is Scapegoat -> {
                character.description = "The Scapegoat is a victim of life and death. When there is a tie during the vote of the day from the village, he/she dies instead and choose who doesn't vote the next day. If he/she's eaten by the werewolves during the night, they got an indigestion and don't eat the next night."
            }
            is Actor -> {
                character.description = "The Actor is a doppleganger. He/she choose another character to copy the role and become this one. That can means there can be two seers, two witches, etc... If the Actor copy a solo role, they become a Duo role and must still fulfill the condition but together."
                character.action = "The Actor choose a character to copy. He/she become this role too and follow the rules associate to it. If it is a solo role, they must win together now."
                character.powerState = PowerState.PERMANENT
            }
            is Angel -> {
                character.description = "The Angel is a divinator. One time per game, he/she can choose to reveal a role to everybody. The character revealed stay alive."
                character.action = "The Angel can choose to reveal the role of a character."
                character.isNocturnal = true
                character.powerState = PowerState.CONSUMABLE
            }
            is RustyKnight -> {
                character.description = "The RustyKnight, if killed during the night, can kill a werewolf from tetanus. The next morning, the alive werewolf the most at the left of the knight will die. This action helps a lot to see innocents between the knight and the killed werewolf. If he/she dies from the village, the closest villager to the right of the knight dies."
            }
            is Servant -> {
                character.description = "The Servant is a promoter. Once a special innocent villager has been killed, she can wakes up during the night and select a character. If it isn't a simple villager, nothing happens. The Servant doesn't see the role. If it is a simple Villager, she can promote the founded to a dead special role. This means a special role can come back in the game. A role like a Witch for example get a refresh of all the actions already done."
                character.action = "The Servant try to find simple villager to promote them of innocent villagers role."
                character.powerState = PowerState.CONSUMABLE
                character.isNocturnal = true
            }
            is StutteringJudge -> {
                character.description = "The StutteringJudge is a dictator. He/She can during the day, one time per game, reveal himself/herself and kill someone. If the victim is a werewolf, he/she become the captain/mayor. Otherwise, he/she dies."
            }
            is Manipulator -> {
                character.description = "The Manipulator is a kind of racist. He/She select a number of players equal to the number of werewolves (information from the game master). If the selected dies, he/she wins. If there is no more werewolves in the game and the Manipulator is alive, he/she wakes up to kill."
                character.action = "The Manipulator choose a number of players equal to the number of werewolves (GM gives the information) that must die in order for him/her to win. If the werewolves are dead, he/she can select a victim to kill."
                character.powerState = PowerState.CONSUMABLE
            }
            is BigBadWolf -> {
                character.description = "The big bad wolf can eat a second victim during the night. He/she eats with the others on the common turn then has a unique turn for him/her."
            }
            is WolfHound -> {
                character.description = "The WolfHound is a cub Wolf. If he/she dies, the werewolves are angry and eat an additional victim the next night."
                character.action = "The WolfHound anger the pack of werewolves if he/she dies and add an additional victim the next night."
                character.powerState = PowerState.PERMANENT
            }
        }
        character.mode = CharacterMode.AUTHOR

    }

    ///
    /// Add a character to the game
    ///
    private fun addCharacterToGame(character: Character) {
        //Copy the object
        val newCharacter : Character = character.clone()

        //Check if there is a special case of existing characters
        val existingCharacter : Character?
            = if((character is LittleGirl && character.mode == CharacterMode.NORMAL)
            || character is Werewolf
            || character is WolfFather
            || character is WolfHound) {
                //Case of werewolf and little girl, they must play at the same time
                charactersSelected.firstOrNull{ x -> (x is LittleGirl && x.mode == CharacterMode.NORMAL)
                        || x is Werewolf
                        || x is WolfFather
                        || x is WolfHound}
        } else {
            //Case same kind of character exist
            charactersSelected.firstOrNull{ x -> x.className == character.className }
        }

        //Special case
        if(existingCharacter != null)
            newCharacter.order = existingCharacter.order //Same order
        else {
            newCharacter.order = if(charactersSelected.isEmpty())  0
                                else charactersSelected.maxOf { x -> x.order } + 1 //At the end
        }


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
            cancelClicking(selectedCharacterInGame!!)
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

        setSelectionButtonEnable(false)
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