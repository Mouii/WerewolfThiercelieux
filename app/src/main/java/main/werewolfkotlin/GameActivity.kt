package main.werewolfkotlin

import Model.*
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.children
import main.werewolfkotlin.databinding.ActivityMainBinding

enum class EnumPhase {
    SLEEPING,
    NIGHT,
    WAKING,
    DAY,
    EVENING
}

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val characters : MutableList<Character> = mutableListOf<Character>()

    private val deadCharacters : MutableList<Character> = mutableListOf<Character>()

    private val imageDimension : Int = 300
    private val listDimension : Int = 1400
    private val imageMargin : Int = 10

    private var strIntro : String = "Démarrer la partie"
    private var strStart : String = "Commencer un nouveau tour"
    private var strNext: String = "Prochain tour"
    private var strMorning: String = "C'est le matin, procédons aux éliminations"
    private var strDay: String = "En journée, le village débat et vote pour éliminer quelqu'un."
    private var strEvening : String = "Ces villageois sont encore en vie; ainsi que leurs ennemis."
    private var strSleep: String = "C'est la nuit, le village s'endort!"

    private var gameInProgress : Boolean = true
    private var currentIndex : Int = 0
    private var isNight : Boolean = false
    private var phase : EnumPhase = EnumPhase.EVENING


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameStatusTextView.text = strIntro
        binding.startNightButton.text = strStart

        characters.add(Werewolf(4, R.drawable.werewolf))
        characters.add(Seer(3, R.drawable.seer))
        characters.add(Witch(5, R.drawable.witch))
        characters.add(Hunter(98, R.drawable.hunter))
        characters.add(Thief(1, R.drawable.thief))
        characters.add(Cupid(2, R.drawable.cupid))
        characters.add(LittleGirl(4, R.drawable.littlegirl))
        characters.add(Villager(99, R.drawable.villager))

        characters.sortedBy { x -> x.order }

        //Main disable
        binding.relativeLayout.visibility = View.INVISIBLE

        binding.startNightButton.setOnClickListener {
            if (gameInProgress) {
                if(phase == EnumPhase.NIGHT)
                    showNextCharacters()
                else
                    nextPhase()
            } else {
                binding.gameStatusTextView.text = "The game is over."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameInProgress = false
    }

    private fun showNextCharacters() {
        val maxKey: Int = characters.maxOf { x -> x.order }
        var activeCharacters : List<Character>
        do {
            currentIndex++
            activeCharacters = characters.filter{ x -> x.order == currentIndex }
        } while((activeCharacters.isEmpty() && currentIndex < maxKey)
            || (activeCharacters.isNotEmpty() && activeCharacters.any{ x -> !x.isNocturnal }))

        if (activeCharacters.isEmpty()) {
            nextPhase()
        } else {
            binding.gridcharacterView.removeAllViewsInLayout()
            binding.gridcharacterView.columnCount = if(activeCharacters.size >= 3) 3 else activeCharacters.size
            setPictures(activeCharacters, false)
            setActions(activeCharacters)
        }
    }

    private fun setActions(characters : List<Character>) {
        var description = ""
        for(character in characters) {
            description += "${character.className}\n${character.action()}\n"
        }
        binding.gameStatusTextView.text = description
    }

    private fun setImagePicture(index: Int, character : Character, withListener: Boolean): ImageView {
        val imageView = ImageView(this).apply {
            setImageResource(character.imageResource)
            adjustViewBounds = true
        }

        val layoutParams = LinearLayout.LayoutParams(imageDimension,imageDimension)
        layoutParams.setMargins(0, 0, imageMargin, imageMargin)
        imageView.layoutParams = layoutParams

        if(withListener) {
            imageView.setOnClickListener {
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

    private fun setPictures(characterList: List<Character>, withListener: Boolean) {
        var index: Int = 0

        // Ajouter dynamiquement des ImageView au LinearLayout
        for (character in characterList) {
            val imageView : ImageView = setImagePicture(character.order, character, withListener)
            binding.gridcharacterView.addView(imageView, index)
            index++
        }

    }

    private fun redrawGridForCompleteList() {
        binding.gridcharacterView.removeAllViewsInLayout()
        setPictures(characters, true)
    }

    private fun removeDeadCharacters() {
        if(deadCharacters.size > 0) {
            for (characterDead in deadCharacters) {
                characters.remove(characterDead)
            }
            deadCharacters.clear()
            redrawGridForCompleteList()
        }

    }

    private fun removeListener() {
        for(image in binding.gridcharacterView.children) {
            image.isEnabled = false
        }
    }

    //region Game Phases

    private fun nextPhase() {
        when(phase) {
            EnumPhase.SLEEPING -> nightPhase()
            EnumPhase.NIGHT -> wakingPhase()
            EnumPhase.WAKING -> dayPhase()
            EnumPhase.DAY -> eveningPhase()
            EnumPhase.EVENING -> sleepingPhase()
        }
    }

    private fun sleepingPhase() {
        phase = EnumPhase.SLEEPING
        isNight = true;
        binding.gameStatusTextView.text = strSleep
        currentIndex = 0 // Reset index for the next night
    }

    private fun nightPhase() {
        phase = EnumPhase.NIGHT
        binding.startNightButton.text = strNext
        showNextCharacters()
    }

    private fun wakingPhase() {
        phase = EnumPhase.WAKING
        isNight = false
        binding.gameStatusTextView.text = strMorning
        binding.gridcharacterView.columnCount = 3
        setPictures(characters, true)
    }

    private fun dayPhase() {
        removeDeadCharacters()
        phase = EnumPhase.DAY
        binding.gameStatusTextView.text = strDay
    }

    private fun eveningPhase() {
        removeDeadCharacters()
        phase = EnumPhase.EVENING
        binding.gameStatusTextView.text = strEvening
        removeListener()
    }

    //endregion GamePhase
}