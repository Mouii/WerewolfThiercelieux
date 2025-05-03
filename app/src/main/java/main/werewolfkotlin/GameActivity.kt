package main.werewolfkotlin

import android.content.Intent
import model.*
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import main.werewolfkotlin.databinding.ActivityGameBinding

/***
 * //Phase of the game
 */
enum class EnumPhase {
    SLEEPING,
    NIGHT,
    DAWN,
    DAY,
    SUNSET
}

/***
 * Map on information to affect and keep. Is only used by activities deployed during the game
 */
val infoMap: MutableMap<Int, String> = mutableMapOf()

/***
 * Contains the list of active roles that are 'consumable'. Is only used by activities deployed during the game
 */
var roleListConsumable : MutableMap<CharacterGame, Boolean>  = mutableMapOf()

class GameActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityGameBinding

    //List of the characters
    private var characterGames : MutableList<CharacterGame> = mutableListOf()

    //List of dead characters
    private val deadCharacterGames : MutableList<CharacterGame> = mutableListOf()

    //Image dimension
    private val imageDimension : Int = 300
    private val imageMargin : Int = 10

    //Game is over or not. Only used for now if activity is badly initialized
    private var gameInProgress : Boolean = true

    //Index for turn order.
    private var currentIndex : Int = -1

    //Phase handler. Start in evening to be in sleeping phase when starting the game
    private var phase : EnumPhase = EnumPhase.SUNSET

    //Turn order reference of werewolf
    private var werewolfTurn : Int = 0

    //Game turn tracing
    private var gameTurn : Int = 0

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameStatusTextView.text = getString(R.string.GameView_StartGame)

        setBackground(phase)

        // Retrieve the JSON string from the Intent
        val jsonString = intent.getStringExtra("GameList")

        // Convert the JSON string back to a MutableList
        val gson = Gson()
        val type = object : TypeToken<List<CharacterGame>>() {}.type
        var chars : List<CharacterGame> = listOf()

        try {
            //Get the list from the JSON
            chars = gson.fromJson(jsonString, type)
        } catch (ex : Exception) {
            println(ex)

        }

        if(chars.isNotEmpty()) {
            deserializeProperly(chars)
        } else {//Bad catch up
            gameInProgress = false
        }

        //Set the listener for the only button
        binding.startNightButton.setOnClickListener {

            //Main use, change night role or game phase
            if (gameInProgress) {
                if(phase == EnumPhase.NIGHT)
                    showNextCharacters()
                else
                    nextPhase()
            } else {
                //Only on bad catch up
                binding.gameStatusTextView.text = getString(R.string.GameView_BadInit)
            }
        }

        updateHeader()

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            finish()
        }


        /*binding.root.setOnTouchListener(object : OnSwipeTouchListener(this@GameActivity) {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onSwipeLeft() {
                sweepLeft()
            }
        })*/

        binding.InformationButton.setOnClickListener {
            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, InformationActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }

        binding.RoleButton.setOnClickListener {
            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, RoleActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }

    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sweepLeft() {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, InformationActivity::class.java)

        //Start the other activity
        startActivity(intent)
    }

    /***
     * On restart override. Allow updating of actual turn in case a change occurred in another activity
     */
    override fun onRestart() {
        super.onRestart()

        var activeCharacterGames : List<CharacterGame>

        if(phase == EnumPhase.NIGHT) {
            //Set the list of active characters from the index
            if(currentIndex == werewolfTurn) {
                activeCharacterGames =  characterGames.filter { x -> x.isWerewolf && x.isNocturnal }

                //If there is only one werewolf at this turn, better skip it
                if(activeCharacterGames.size == 1 && (activeCharacterGames[0] is BigBadWolf || activeCharacterGames[0] is WhiteWerewolf))
                    activeCharacterGames = emptyList()

            } else {
                activeCharacterGames =  characterGames.filter { x -> x.order == currentIndex && x.isNocturnal }
            }

            //End of list, empty active characters next phase
            if (activeCharacterGames.isEmpty()) {
                showNextCharacters()
            } else {

                //Show the active characters with their descriptions
                setPictures(activeCharacterGames, false)
                setActions(activeCharacterGames)
            }
        }
    }

    /***
     * Override of destroying
     */
    override fun onDestroy() {
        super.onDestroy()
        infoMap.clear()
        roleListConsumable.clear()
    }

    /***
     * Create the good list of characters
     * GSon doesn't handle polymorphism, so we recreate each object
     */
    private fun deserializeProperly(chars : List<CharacterGame>) {
        chars.forEach { character ->

            val characterRedefined =
                WorkerEasier.getGoodCharacterCast(character, character.className)

            characterGames.add(characterRedefined)

        }
        //Order the list again just in case
        characterGames.sortBy { x -> x.order }

        characterGames.filter { x -> x.powerState == PowerState.CONSUMABLE }.forEach { character ->
            roleListConsumable[character] = false
        }

        if(roleListConsumable.isEmpty())
            binding.RoleButton.isEnabled = false

        //Order reference : werewolf > wolfFather > bigBadWolf > wolfHound > others
        val characterGameReference : CharacterGame? = characterGames.firstOrNull{ x ->
                    x is Werewolf
                    || x is WolfFather
                    || x is BigBadWolf
                    || x is WolfHound
                    || x is WhiteWerewolf
                    || x.isWerewolf
        }

        //Set special pack wolf turn
        if(characterGameReference != null) {
            werewolfTurn = characterGameReference.order
        }

    }

    /***
     * Show the next night characters
     */
    private fun showNextCharacters() {
        //Always get max order
        val maxKey: Int = characterGames.maxOf { x -> x.order }
        var activeCharacterGames : List<CharacterGame>
        do {
            //Upgrade index
            currentIndex++

            //Set the list of active characters from the index
            if(currentIndex == werewolfTurn) {
                activeCharacterGames =  characterGames.filter { x -> x.isWerewolf && x.isNocturnal }

                //If there is only one werewolf at this turn, better skip it
                if(activeCharacterGames.size == 1 && (activeCharacterGames[0] is BigBadWolf || activeCharacterGames[0] is WhiteWerewolf))
                    activeCharacterGames = emptyList()

            } else {
                activeCharacterGames =  characterGames.filter { x -> x.order == currentIndex }
            }

        //Two cases to finish the loop, reaching the end of list,
        //Or having all characters playing at night
        } while((activeCharacterGames.isEmpty() && currentIndex < maxKey)
            || (activeCharacterGames.isNotEmpty() && activeCharacterGames.any{ x -> !x.isNocturnal }))

        //End of list, empty active characters next phase
        if (activeCharacterGames.isEmpty()) {
            nextPhase()
        } else {

            if(activeCharacterGames.size == 1 && activeCharacterGames[0].className == "WildChild") {
                setupWildChild(activeCharacterGames[0])
            }

            //Show the active characters with their descriptions
            setPictures(activeCharacterGames, false)
            setActions(activeCharacterGames)
        }
    }

    /***
     * Fulfill the different actions
     */
    private fun setActions(characterGames : List<CharacterGame>) {
        //Basic string
        var description = ""

        //Append each strict with EACH class
        for(character in characterGames.distinctBy { x -> x.name }) {
            description += "${character.name}\n${character.action()}\n\n"
        }
        binding.gameStatusTextView.text = description
    }

    /***
     * Set the imageView to show
     */
    private fun setImagePicture(characterGame : CharacterGame, withListener: Boolean): ImageView {
        //Create the imageView with some parameters
        val imageView = ImageView(this).apply {
            @DrawableRes val img = WorkerEasier.getCharacterImage(characterGame)
            setImageResource(img)
            adjustViewBounds = true
        }

        //Adding layout parameters
        val layoutParams = LinearLayout.LayoutParams(imageDimension,imageDimension)
        layoutParams.setMargins(0, 0, imageMargin, imageMargin)
        imageView.layoutParams = layoutParams

        //Only with listeners configuration we add it
        if(withListener) {
            imageView.setOnClickListener {
                //Add characters to the dead list and change the padding
                if(deadCharacterGames.contains(characterGame)) {
                    deadCharacterGames.remove(characterGame)
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    deadCharacterGames.add(characterGame)
                    imageView.setPadding(10, 10, 10, 10)
                    imageView.setBackgroundColor(Color.GREEN)
                }

            }
        }

        return imageView
    }

    /***
     * Set all the pictures of the gridlayout
     */
    private fun setPictures(characterGameList: List<CharacterGame>, withListener: Boolean) {
        //Refresh the gridLayout by clearing it
        binding.gridCharacterView.removeAllViewsInLayout()

        if(characterGameList.size > 1) {
            // Add each imageView to the gridLayout
            for ((index, character) in characterGameList.withIndex()) {
                val imageView : ImageView = setImagePicture(character, withListener)
                binding.gridCharacterView.addView(imageView, index)
            }
        } else if (characterGameList.size == 1) {
            val imageView : ImageView  = setImagePicture(characterGameList[0], withListener)
            imageView.layoutParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(0, 3) // Start at row 0, span 2 rows
                columnSpec = GridLayout.spec(0, 3) // Start at column 0, span 2 columns
                setGravity(Gravity.FILL)
            }
            binding.gridCharacterView.addView(imageView, 0)
        }


    }

    /***
     * Set the background image
     */
    private fun setBackground(phase: EnumPhase) {
        binding.root.setBackgroundResource(WorkerEasier.getBackgroundImage(phase))
    }

    /***
     * Update the header with turn and phase
     */
    private fun updateHeader() {
        binding.turnPhase.text = String.format(getString(R.string.GameView_Header), WorkerEasier.mapPhaseTranslation[phase], gameTurn)
    }

    /***
     * Remove the dead from the active list
     */
    private fun removeDeadCharacters() {
        if(deadCharacterGames.size > 0) {
            for (characterDead in deadCharacterGames) {
                characterGames.remove(characterDead)

                //Update specific consumable list
                if(roleListConsumable.contains(characterDead))
                    roleListConsumable.remove(characterDead)
            }

            //Special updates
            specialUpdatesAfterDeath();

            //Always clear the dead list
            deadCharacterGames.clear()
        }

        //If the user empty the list, game over
        if(characterGames.size == 0) {
            gameInProgress = false
            binding.gameStatusTextView.text = getString(R.string.GameView_GameOver)
            binding.startNightButton.isEnabled = false
        }

    }

    /***
     * Activate the special conditions
     */
    private fun specialUpdatesAfterDeath() {
        characterGames.filter { it.powerState == PowerState.CONDITIONAL }.forEach { x ->
            when(x.condition) {
                ConditionalActivation.LINKEDROLES -> {
                    if(deadCharacterGames.map{ it.className}.toSet().intersect(x.rolesToStick.toSet()).isNotEmpty()) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }

                ConditionalActivation.ONEWEREWOLF -> {
                    if(deadCharacterGames.any { it.isWerewolf }) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }
                ConditionalActivation.ONEVILLAGER -> {
                    if(deadCharacterGames.any { !it.isWerewolf }) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }
                ConditionalActivation.ALLWEREWOLVES -> {
                    if(!characterGames.any { it.isWerewolf }) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }
                ConditionalActivation.ALLVILLAGERS -> {
                    if(characterGames.filter { it != x }.all { it.isWerewolf }) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }
                ConditionalActivation.ISALONE -> {
                    if(characterGames.filter { it.className == x.className }.size == 1) {
                        x.isNocturnal = !x.originalNightState!! //Always the reverse
                        x.powerState = PowerState.PERMANENT//Become permanent
                    }
                }
                ConditionalActivation.NOCONDITION -> {}
            }
        }

    }

    /***
     * Setup the wild child for the turn of werewolf but not awaken
     */
    private fun setupWildChild(child: CharacterGame) {
        child.order = werewolfTurn
        child.isWerewolf = true
        child.isNocturnal = false
    }

    /***
     * Global function handling all phases
     */
    private fun nextPhase() {
        when(phase) {
            EnumPhase.SLEEPING -> nightPhase()
            EnumPhase.NIGHT -> wakingPhase()
            EnumPhase.DAWN -> dayPhase()
            EnumPhase.DAY -> eveningPhase()
            EnumPhase.SUNSET -> sleepingPhase()
        }
    }

    /***
     * Actions on sleeping Phase
     */
    private fun sleepingPhase() {
        phase = EnumPhase.SLEEPING //Set the phase
        setBackground(phase)
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseSleeping) // Change text
        currentIndex = -1 // Reset index for the next night
        gameTurn++ //Starting new turn
        updateHeader()
        binding.gridCharacterView.removeAllViewsInLayout() //No characters showed
    }

    /***
     * Actions on night Phase
     */
    private fun nightPhase() {
        phase = EnumPhase.NIGHT // Night phase
        setBackground(phase)
        updateHeader()
        showNextCharacters() // Start showing night roles at the first moment
    }

    /***
     * Actions on waking Phase
     */
    private fun wakingPhase() {
        phase = EnumPhase.DAWN // Waking phase
        setBackground(phase)
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseDawn) // Morning message
        updateHeader()
        setPictures(characterGames, true) // Set the pictures WITH listeners
    }

    /***
     * Actions on day Phase
     */
    private fun dayPhase() {
        phase = EnumPhase.DAY // Day phase
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseDay)// Day message
        setBackground(phase)
        removeDeadCharacters() // Remove characters dead by night
        updateHeader()
        setPictures(characterGames, true) // Set pictures WITH Listeners
    }

    /***
     * Actions on evening Phase
     */
    private fun eveningPhase() {
        phase = EnumPhase.SUNSET // Evening phase
        binding.gameStatusTextView.text = getString(R.string.GameView_phaseEvening) // Evening message
        setBackground(phase)
        removeDeadCharacters() // Remove the dead
        updateHeader()
        setPictures(characterGames, false) // Set pictures WITHOUT listeners
    }

}