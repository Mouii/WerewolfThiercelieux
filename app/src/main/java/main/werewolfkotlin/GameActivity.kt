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

//Phase of the game
enum class EnumPhase {
    SLEEPING,
    NIGHT,
    DAWN,
    DAY,
    SUNSET
}

class GameActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityGameBinding

    //List of the characters
    private var characters : MutableList<Character> = mutableListOf()

    //List of dead characters
    private val deadCharacters : MutableList<Character> = mutableListOf()

    //Image dimension
    private val imageDimension : Int = 300
    private val imageMargin : Int = 10

    //All text (to change to adapt language)
    private var strIntro : String = "Start a game"
    private var strStart : String = "Begin the game"
    private var strNext: String = "Next turn"
    private var strEnd : String = "End the game"
    private var strMorning: String = "Morning! Let's proceed to the eliminations"
    private var strDay: String = "Day time! Chat and speak about what to do!"
    private var strEvening : String = "Dinner is served! But watch out, these villagers are alive... so are their enemies..."
    private var strSleep: String = "This is the night! The village goes to sleep!"

    //Game is over or not. Only used for now if activity is badly initialized
    private var gameInProgress : Boolean = true

    //Index for turn order.
    private var currentIndex : Int = -1

    //Phase handler. Start in evening to be in sleeping phase when starting the game
    private var phase : EnumPhase = EnumPhase.SUNSET

    //Turn order reference of werewolf
    private var werewolfTurn : Int = 0

    //Special map for special characters like bigBadWolf, whiteWerewolf
    //that have special conditions on some parts
    private val chartCharacters : MutableMap<String, Int> = mutableMapOf()

    //Game turn tracing
    private var gameTurn : Int = 0

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameStatusTextView.text = strIntro
        binding.startNightButton.text = strStart
        binding.endButton.text = strEnd

        setBackground(phase)

        // Retrieve the JSON string from the Intent
        val jsonString = intent.getStringExtra("GameList")

        // Convert the JSON string back to a MutableList
        val gson = Gson()
        val type = object : TypeToken<List<Character>>() {}.type
        var chars : List<Character> = listOf()

        try {
            //Get the list from the JSON
            chars = gson.fromJson(jsonString, type)
        } catch (ex : Exception) {
            println(ex)

        }

        if(chars.isNotEmpty()) {
            deserializeProperly(chars)
        } else {//Bad catch up, keep the test list by default for now
            characters.add(Werewolf(4))
            characters.add(Seer(3))
            characters.add(Witch(5))
            characters.add(Hunter(98))
            characters.add(Thief(1))
            characters.add(Cupid(2))
            characters.add(LittleGirl(4))
            characters.add(Villager(99))
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
                binding.gameStatusTextView.text = "No game."
            }
        }

        updateHeader()

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            finish()
        }


        binding.mainLayout.setOnTouchListener(object : OnSwipeTouchListener(this@GameActivity) {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onSwipeLeft() {
                sweepLeft()
            }
        })


    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sweepLeft() {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, InformationActivity::class.java)

        //Start the other activity
        startActivity(intent)
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_from_right,
            R.anim.slide_out_to_left)
    }

    ///
    /// Create the good list of characters
    /// GSon doesn't handle polymorphism, so we recreate each object
    ///
    private fun deserializeProperly(chars : List<Character>) {
        chars.forEach {character ->
            when(character.className) {
                "Actor" -> characters.add(Actor(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode
                ))
                "Angel" -> characters.add(Angel(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "BearTamer" -> characters.add(BearTamer(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "BigBadWolf" ->  {
                    characters.add(BigBadWolf(character.description
                        , character.action
                        , character.isNocturnal
                        , character.powerState
                        , character.isWerewolf
                        , character.order
                        , character.maxOccurrence
                        , character.mode))
                    chartCharacters[character.className] = character.order
                }
                "Brother" -> characters.add(Brother(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Crow" -> characters.add(Crow(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Cupid" -> characters.add(Cupid(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Defender" -> characters.add(Defender(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Elder" -> characters.add(Elder(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Fireman" -> characters.add(Fireman(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Fox" -> characters.add(Fox(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Gypsy" -> characters.add(Gypsy(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Hunter" -> characters.add(Hunter(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Idiot" -> characters.add(Idiot(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "LittleGirl" -> characters.add(LittleGirl(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Manipulator" -> characters.add(Manipulator(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Piper" -> characters.add(Piper(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "RustyKnight" -> characters.add(RustyKnight(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Scapegoat" -> characters.add(Scapegoat(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Seer" -> characters.add(Seer(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Servant" -> characters.add(Servant(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Sister" -> characters.add(Sister(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "StutteringJudge" -> characters.add(StutteringJudge(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Thief" -> characters.add(Thief(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Villager" -> characters.add(Villager(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "VillagerVillager" -> characters.add(VillagerVillager(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "Werewolf" -> characters.add(Werewolf(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "WhiteWerewolf" -> {
                    characters.add(WhiteWerewolf(character.description
                        , character.action
                        , character.isNocturnal
                        , character.powerState
                        , character.isWerewolf
                        , character.order
                        , character.maxOccurrence
                        , character.mode))
                    chartCharacters[character.className] = character.order
                }
                "WildChild" -> {
                    characters.add(WildChild(character.description
                        , character.action
                        , character.isNocturnal
                        , character.powerState
                        , character.isWerewolf
                        , character.order
                        , character.maxOccurrence
                        , character.mode))
                    chartCharacters[character.className] = character.order
                }
                "Witch" -> characters.add(Witch(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "WolfFather" -> characters.add(WolfFather(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
                "WolfHound" -> characters.add(WolfHound(character.description
                    , character.action
                    , character.isNocturnal
                    , character.powerState
                    , character.isWerewolf
                    , character.order
                    , character.maxOccurrence
                    , character.mode))
            }
        }

        //Order the list again just in case
        characters.sortBy { x -> x.order }

        //Order reference : werewolf > wolfFather > bigBadWolf > wolfHound > others
        val characterReference : Character? = characters.firstOrNull{ x ->
                    x is Werewolf
                    || x is WolfFather
                    || x is BigBadWolf
                    || x is WolfHound
                    || x is WhiteWerewolf
                    || x.isWerewolf
        }

        //Set special pack wolf turn
        if(characterReference != null) {
            werewolfTurn = characterReference.order
        }


    }

    ///
    /// Show the next night characters
    ///
    private fun showNextCharacters() {
        //Always get max order
        val maxKey: Int = characters.maxOf { x -> x.order }
        var activeCharacters : List<Character>
        do {
            //Upgrade index
            currentIndex++

            //Set the list of active characters from the index
            activeCharacters = if(currentIndex == werewolfTurn) {
                characters.filter { x -> x.isWerewolf }
            } else {
                characters.filter { x -> x.order == currentIndex }
            }
        //Two cases to finish the loop, reaching the end of list,
        //Or having all characters playing at night
        } while((activeCharacters.isEmpty() && currentIndex < maxKey)
            || (activeCharacters.isNotEmpty() && activeCharacters.any{ x -> !x.isNocturnal }))

        //End of list, empty active characters next phase
        if (activeCharacters.isEmpty()) {
            nextPhase()
        } else {
            //Show the active characters with their descriptions
            setPictures(activeCharacters, false)
            setActions(activeCharacters)
        }
    }

    ///
    /// Fulfill the different actions
    ///
    private fun setActions(characters : List<Character>) {
        //Basic string
        var description = ""

        //Append each strict with EACH class
        for(character in characters.distinctBy { x -> x.className }) {
            description += "${character.className}\n${character.action()}\n\n"
        }
        binding.gameStatusTextView.text = description
    }

    ///
    /// Set the imageView to show
    ///
    private fun setImagePicture(character : Character, withListener: Boolean): ImageView {
        //Create the imageView with some parameters
        val imageView = ImageView(this).apply {
            @DrawableRes val img = ImageGetter.getCharacterImage(character)
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
                if(deadCharacters.contains(character)) {
                    deadCharacters.remove(character)
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    deadCharacters.add(character)
                    imageView.setPadding(10, 10, 10, 10)
                    imageView.setBackgroundColor(Color.GREEN)
                }

            }
        }

        return imageView
    }

    ///
    /// Set all the pictures of the gridlayout
    ///
    private fun setPictures(characterList: List<Character>, withListener: Boolean) {
        //Refresh the gridLayout by clearing it
        binding.gridcharacterView.removeAllViewsInLayout()

        if(characterList.size > 1) {
            // Add each imageView to the gridLayout
            for ((index, character) in characterList.withIndex()) {
                val imageView : ImageView = setImagePicture(character, withListener)
                binding.gridcharacterView.addView(imageView, index)
            }
        } else if (characterList.size == 1) {
            val imageView : ImageView  = setImagePicture(characterList[0], withListener)
            imageView.layoutParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(0, 3) // Start at row 0, span 2 rows
                columnSpec = GridLayout.spec(0, 3) // Start at column 0, span 2 columns
                setGravity(Gravity.FILL)
            }
            binding.gridcharacterView.addView(imageView, 0)
        }


    }

    ///
    /// Set the background image
    ///
    private fun setBackground(phase: EnumPhase) {
        binding.root.setBackgroundResource(ImageGetter.getBackgroundImage(phase))
        binding.root.background.alpha = 128
    }

    ///
    /// Update the header with turn and phase
    ///
    private fun updateHeader() {
        binding.turnPhase.text = "Phase $phase Turn ${gameTurn}"
    }

    ///
    /// Remove the dead from the active list
    ///
    private fun removeDeadCharacters() {
        if(deadCharacters.size > 0) {
            for (characterDead in deadCharacters) {
                characters.remove(characterDead)
            }

            //Special updates
            specialUpdatesAfterDeath();

            //Always clear the dead list
            deadCharacters.clear()
        }

        //If the user empty the list, game over
        if(characters.size == 0) {
            gameInProgress = false
            binding.gameStatusTextView.text = "Partie terminée."
            binding.startNightButton.isEnabled = false
        }

    }

    ///
    /// Delete the second turn of special werewolves
    ///
    private fun checkAndResetOrderOfSpecialWerewolf(className: String) {
        if(chartCharacters.containsKey(className)
            && chartCharacters[className] != werewolfTurn) {
            val characterWolf = characters.firstOrNull { x -> x.className == className }

            if (characterWolf != null && characterWolf.order != werewolfTurn) {
                characterWolf.order = werewolfTurn
                chartCharacters[characterWolf.className] = werewolfTurn
            }
        }
    }

    ///
    /// Cut the call of brothers or sisters if they are alone
    ///
    private fun stopBrotherSister(className: String) {
        val listCoop = characters.filter { x -> x.className == className }

        if(listCoop.isNotEmpty() && listCoop.size == 1)
            listCoop[0].isNocturnal = false
    }

    ///
    /// Activate the special conditions
    ///
    private fun specialUpdatesAfterDeath() {

        deadCharacters.forEach { x ->
            if(chartCharacters.containsKey(x.className)) {
                chartCharacters.remove(x.className)
            }
        }

        if(characters.any { x -> x is BigBadWolf && x.mode == CharacterMode.NORMAL}
            && deadCharacters.any { x -> x.isWerewolf }) {
            checkAndResetOrderOfSpecialWerewolf("BigBadWolf")
        }

        if(!characters.any { x -> x !is WhiteWerewolf && x.isWerewolf}) {
            checkAndResetOrderOfSpecialWerewolf("WhiteWerewolf")
        }

        if(deadCharacters.any { x -> x is Brother }) {
            stopBrotherSister("Brother")
        }

        if(deadCharacters.any { x -> x is Sister }) {
            stopBrotherSister("Sister")
        }

    }

    ///
    /// Exclusive function, might disappear later => setup the wild child as a werewolf
    /// even if he's not one
    ///
    private fun setupWildChild() {
        if(chartCharacters.containsKey("WildChild") && gameTurn == 1) {
            val child = characters.first { x -> x is WildChild }
            child.order = werewolfTurn
            child.isWerewolf = true
            chartCharacters["WildChild"] = werewolfTurn
        }
    }

    ///
    /// Global function handling all phases
    ///
    private fun nextPhase() {
        when(phase) {
            EnumPhase.SLEEPING -> nightPhase()
            EnumPhase.NIGHT -> wakingPhase()
            EnumPhase.DAWN -> dayPhase()
            EnumPhase.DAY -> eveningPhase()
            EnumPhase.SUNSET -> sleepingPhase()
        }
    }

    ///
    /// Actions on sleeping Phase
    ///
    private fun sleepingPhase() {
        phase = EnumPhase.SLEEPING //Set the phase
        setBackground(phase)
        binding.gameStatusTextView.text = strSleep // Change text
        binding.startNightButton.text = strNext // Change text
        currentIndex = -1 // Reset index for the next night
        gameTurn++ //Starting new turn
        updateHeader()
        binding.gridcharacterView.removeAllViewsInLayout() //No characters showed
    }

    ///
    /// Actions on night Phase
    ///
    private fun nightPhase() {
        phase = EnumPhase.NIGHT // Night phase
        setBackground(phase)
        updateHeader()
        showNextCharacters() // Start showing night roles at the first moment
    }

    ///
    /// Actions on waking Phase
    ///
    private fun wakingPhase() {
        phase = EnumPhase.DAWN // Waking phase
        setupWildChild() //Only for first turn
        setBackground(phase)
        binding.gameStatusTextView.text = strMorning // Morning message
        updateHeader()
        setPictures(characters, true) // Set the pictures WITH listeners
    }

    ///
    /// Actions on day Phase
    ///
    private fun dayPhase() {
        phase = EnumPhase.DAY // Day phase
        binding.gameStatusTextView.text = strDay // Day message
        setBackground(phase)
        removeDeadCharacters() // Remove characters dead by night
        updateHeader()
        setPictures(characters, true) // Set pictures WITH Listeners
    }

    ///
    /// Actions on evening Phase
    ///
    private fun eveningPhase() {
        phase = EnumPhase.SUNSET // Evening phase
        binding.gameStatusTextView.text = strEvening // Evening message
        setBackground(phase)
        removeDeadCharacters() // Remove the dead
        updateHeader()
        setPictures(characters, false) // Set pictures WITHOUT listeners
    }

}