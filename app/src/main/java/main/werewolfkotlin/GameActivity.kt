package main.werewolfkotlin

import Model.*
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.children
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import main.werewolfkotlin.databinding.ActivityGameBinding

enum class EnumPhase {
    SLEEPING,
    NIGHT,
    WAKING,
    DAY,
    EVENING
}

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private var characters : MutableList<Character> = mutableListOf()

    private val deadCharacters : MutableList<Character> = mutableListOf()

    private val imageDimension : Int = 300
    private val listDimension : Int = 1400
    private val imageMargin : Int = 10

    private var strIntro : String = "Démarrer la partie"
    private var strStart : String = "Commencer un nouveau tour"
    private var strNext: String = "Prochain tour"
    private var strEnd : String = "Fin de partie"
    private var strMorning: String = "C'est le matin, procédons aux éliminations"
    private var strDay: String = "En journée, le village débat et vote pour éliminer quelqu'un."
    private var strEvening : String = "Ces villageois sont encore en vie; ainsi que leurs ennemis."
    private var strSleep: String = "C'est la nuit, le village s'endort!"

    private var gameInProgress : Boolean = true
    private var currentIndex : Int = -1
    private var isNight : Boolean = false
    private var phase : EnumPhase = EnumPhase.EVENING


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameStatusTextView.text = strIntro
        binding.startNightButton.text = strStart
        binding.endButton.text = strEnd

        // Retrieve the JSON string from the Intent
        val jsonString = intent.getStringExtra("GameList")

        // Convert the JSON string back to a MutableList
        val gson = Gson()
        val type = object : TypeToken<List<Character>>() {}.type
        var chars : List<Character> = listOf()

        try {
            chars = gson.fromJson(jsonString, type)
        } catch (ex : Exception) {
            println(ex)

        }

        if(chars.isNotEmpty()) {
            deserializeProperly(chars)
        } else {
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

        characters.sortBy { x -> x.order }

        binding.startNightButton.setOnClickListener {
            if (gameInProgress) {
                if(phase == EnumPhase.NIGHT)
                    showNextCharacters()
                else
                    nextPhase()
            } else {
                binding.gameStatusTextView.text = "No game."
            }
        }

        binding.endButton.setOnClickListener {
            finish()
        }
    }

    private fun deserializeProperly(chars : List<Character>) {
        chars.forEach {character ->
            when(character.className) {
                "Cupid" -> characters.add(Cupid(character.order))
                "Hunter" -> characters.add(Hunter(character.order))
                "LittleGirl" -> characters.add(LittleGirl(character.order))
                "Seer" -> characters.add(Seer(character.order))
                "Thief" -> characters.add(Thief(character.order))
                "Villager" -> characters.add(Villager(character.order))
                "Werewolf" -> characters.add(Werewolf(character.order))
                "Witch" -> characters.add(Witch(character.order))
            }
        }
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

    private fun setImagePicture(character : Character, withListener: Boolean): ImageView {
        val imageView = ImageView(this).apply {
            @DrawableRes val img = ImageGetter.GetImage(character)
            setImageResource(img)
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
        binding.gridcharacterView.removeAllViewsInLayout()

        // Ajouter dynamiquement des ImageView au LinearLayout
        for ((index, character) in characterList.withIndex()) {
            val imageView : ImageView = setImagePicture(character, withListener)
            binding.gridcharacterView.addView(imageView, index)
        }

    }

    private fun removeDeadCharacters() {
        if(deadCharacters.size > 0) {
            for (characterDead in deadCharacters) {
                characters.remove(characterDead)
            }
            deadCharacters.clear()
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
        isNight = true
        binding.gameStatusTextView.text = strSleep
        currentIndex = -1 // Reset index for the next night
        binding.gridcharacterView.removeAllViewsInLayout()
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