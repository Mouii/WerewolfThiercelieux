package main.werewolfkotlin

import Model.*
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
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
    private var strIntro : String = "Démarrer la partie"
    private var strStart : String = "Commencer un nouveau tour"
    private var strNext: String = "Prochain tour"
    private var strEnd : String = "Fin de partie"
    private var strMorning: String = "C'est le matin, procédons aux éliminations"
    private var strDay: String = "En journée, le village débat et vote pour éliminer quelqu'un."
    private var strEvening : String = "Ces villageois sont encore en vie; ainsi que leurs ennemis."
    private var strSleep: String = "C'est la nuit, le village s'endort!"

    //Game is over or not. Only used for now if activity is badly initialized
    private var gameInProgress : Boolean = true

    //Index for turn order.
    private var currentIndex : Int = -1

    //Night part. Will have more use for background and drawing
    private var isNight : Boolean = false

    //Phase handler. Start in evening to be in sleeping phase when starting the game
    private var phase : EnumPhase = EnumPhase.SUNSET

    private var werewolfTurn : Int = 0

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

        //Special button to finish the game whenever we want
        binding.endButton.setOnClickListener {
            finish()
        }
    }

    ///
    /// Create the good list of characters
    /// GSon doesn't handle polymorphism, so we recreate each object
    ///
    private fun deserializeProperly(chars : List<Character>) {
        chars.forEach {character ->
            when(character.className) {
                "Actor" -> characters.add(Actor(character.order))
                "Angel" -> characters.add(Angel(character.order))
                "BearTamer" -> characters.add(BearTamer(character.order))
                "BigBadWolf" -> characters.add(BigBadWolf(character.order))
                "Brother" -> characters.add(Brother(character.order))
                "Cupid" -> characters.add(Cupid(character.order))
                "Defender" -> characters.add(Defender(character.order))
                "Elder" -> characters.add(Elder(character.order))
                "Fox" -> characters.add(Fox(character.order))
                "Gypsy" -> characters.add(Gypsy(character.order))
                "Hunter" -> characters.add(Hunter(character.order))
                "Idiot" -> characters.add(Idiot(character.order))
                "LittleGirl" -> characters.add(LittleGirl(character.order))
                "Manipulator" -> characters.add(Manipulator(character.order))
                "Piper" -> characters.add(Piper(character.order))
                "RustyKnight" -> characters.add(RustyKnight(character.order))
                "Scapegoat" -> characters.add(Scapegoat(character.order))
                "Seer" -> characters.add(Seer(character.order))
                "Servant" -> characters.add(Servant(character.order))
                "Sister" -> characters.add(Sister(character.order))
                "StutteringJudge" -> characters.add(StutteringJudge(character.order))
                "Thief" -> characters.add(Thief(character.order))
                "Villager" -> characters.add(Villager(character.order))
                "VillagerVillager" -> characters.add(VillagerVillager(character.order))
                "Werewolf" -> characters.add(Werewolf(character.order))
                "WhiteWerewolf" -> characters.add(WhiteWerewolf(character.order))
                "WildChild" -> characters.add(WildChild(character.order))
                "Witch" -> characters.add(Witch(character.order))
                "WolfFather" -> characters.add(WolfFather(character.order))
                "WolfHound" -> characters.add(WolfHound(character.order))
            }
        }

        //Order the list again just in case
        characters.sortBy { x -> x.order }

        val characterReference : Character? = characters.firstOrNull{ x ->
                    x is Werewolf
                    || x is WolfFather
                    || x is BigBadWolf
                            || x.isWerewolf
                            }

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
            activeCharacters = characters.filter{ x -> x.order == currentIndex }

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
            description += "${character.className}\n${character.action()}\n"
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

    private fun setBackground(phase: EnumPhase) {
        binding.root.setBackgroundResource(ImageGetter.getBackgroundImage(phase))
        binding.root.background.alpha = 128
    }

    ///
    /// Remove the dead from the active list
    ///
    private fun removeDeadCharacters() {
        if(deadCharacters.size > 0) {
            for (characterDead in deadCharacters) {
                characters.remove(characterDead)
            }

            //Always clear the dead list
            deadCharacters.clear()
        }

        if(characters.size == 0) {
            gameInProgress = false
            binding.gameStatusTextView.text = "Partie terminée."
            binding.startNightButton.isEnabled = false
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
        isNight = true //We are in night
        binding.gameStatusTextView.text = strSleep // Change text
        currentIndex = -1 // Reset index for the next night
        binding.gridcharacterView.removeAllViewsInLayout() //No characters showed
    }

    ///
    /// Actions on night Phase
    ///
    private fun nightPhase() {
        phase = EnumPhase.NIGHT // Night phase
        setBackground(phase)
        binding.startNightButton.text = strNext // Change text
        showNextCharacters() // Start showing night roles at the first moment
    }

    ///
    /// Actions on waking Phase
    ///
    private fun wakingPhase() {
        phase = EnumPhase.DAWN // Waking phase
        setBackground(phase)
        isNight = false // No more in night
        binding.gameStatusTextView.text = strMorning // Morning message
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
        setPictures(characters, false) // Set pictures WITHOUT listeners
    }

}