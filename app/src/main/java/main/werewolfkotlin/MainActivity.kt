package main.werewolfkotlin

import Model.*
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
import androidx.core.view.iterator
import com.google.gson.Gson
import main.werewolfkotlin.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val charactersMenu : MutableList<Character> = mutableListOf()

    private val charactersSelected : MutableList<Character> = mutableListOf()

    private val imageDimension : Int = 240
    private val imageMargin : Int = 10

    private var selectedCharacterInGame: Character? = null

    private var characterChart : MutableList<CharacterChart> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val villager = Villager(1)
        val werewolf = Werewolf(2)
        val seer = Seer(3)
        val witch = Witch(4)
        val hunter = Hunter(5)
        val littleGirl = LittleGirl(6)
        val thief = Thief(7)
        val cupid = Cupid(8)

        charactersMenu.add(villager)
        charactersMenu.add(werewolf)
        charactersMenu.add(seer)
        charactersMenu.add(witch)
        charactersMenu.add(hunter)
        charactersMenu.add(littleGirl)
        charactersMenu.add(thief)
        charactersMenu.add(cupid)

        charactersMenu.sortBy { x -> x.order }

        for ((index, character) in charactersMenu.withIndex()) {


            val imageView : ImageView = setImagePicture(character)

            characterChart.add(CharacterChart(character.className,
                0, character.maxOccurence, imageView))

            imageView.setOnClickListener {
                //Add character to selection
                addCharacterToGame(character)

                //Update view
                val charChart = characterChart.first{ x -> x.name == character.className}
                charChart.occurence += 1

                if(charChart.occurence == charChart.maxOccurence) {
                    imageView.isEnabled = false
                    imageView.alpha = 0.5f
                }


                refreshSelectedListCharacters()
            }

            binding.gridSelectView.addView(imageView, index)

        }

        setButtonListenersOfSelected()
        setSelectionButtonEnable(false)
    }

    override fun onRestart() {
        super.onRestart()
        recreate() // This will restart the activity
    }

    private fun setImagePicture(character : Character): ImageView {
        val imageView = ImageView(this).apply {
            @DrawableRes val img = ImageGetter.GetImage(character)
            setImageResource(img)
            adjustViewBounds = true
        }

        val layoutParams = LinearLayout.LayoutParams(imageDimension,imageDimension)
        layoutParams.setMargins(0, 0, imageMargin, imageMargin)
        imageView.layoutParams = layoutParams

        return imageView
    }

    private fun setSelectionButtonEnable(enable : Boolean) {
        binding.leftArrowButton.isEnabled = enable
        binding.rightArrowButton.isEnabled = enable
        binding.cancelButton.isEnabled = enable
        binding.removeButton.isEnabled = enable
    }

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

    private fun refreshSelectedListCharacters() {
        binding.gridSelectedView.removeAllViewsInLayout()

        charactersSelected.sortBy{ x -> x.order }

        for ((index, character) in charactersSelected.distinctBy{ x -> x.className}.withIndex()) {

            // Create a FrameLayout to hold ImageView and TextView
            val frameLayout = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }
                tag = character.className
            }

            val imageView : ImageView = setImagePicture(character)

            frameLayout.addView(imageView)

            val countTextView = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, 4, 4) // Position in bottom-right corner
                }
                text = "x${charactersSelected.count { x -> x.className == character.className}}" // Set your text
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                background = ContextCompat.getDrawable(this@MainActivity, R.color.white)
                setPadding(4, 4, 4, 4) // Add padding
            }

            frameLayout.addView(countTextView)

            imageView.setOnClickListener {
                if(selectedCharacterInGame?.className != character.className) {
                    selectedCharacterInGame = character
                    setSelectionButtonEnable(true)
                    drawSelection(character)
                } else {
                    setSelectionButtonEnable(false)
                    removeSelectedCharacterInGame()
                }

                refreshArrowSelectionSelected()
            }

            binding.gridSelectedView.addView(frameLayout, index)

            if(selectedCharacterInGame?.className == character.className) {
                drawSelection(character)
            }
        }

        refreshArrowSelectionSelected()
    }

    private fun addCharacterToGame(character: Character) {
        val newCharacter : Character = character.clone()
        val existingCharacter : Character?
            = if(character is LittleGirl || character is Werewolf) {
            charactersSelected.firstOrNull{ x -> x.className == "LittleGirl" || x.className == "Werewolf" }
        } else
            charactersSelected.firstOrNull{ x -> x.className == character.className }

        //Special case
        if(existingCharacter != null)
            newCharacter.order = existingCharacter.order
        else
            newCharacter.order = charactersSelected.size

        charactersSelected.add(newCharacter)
    }

    private fun removeCharacterToSelection() {
        if(selectedCharacterInGame != null) {
            charactersSelected.remove(selectedCharacterInGame)

            val charChart = characterChart.first{ x -> x.name == selectedCharacterInGame!!.className}
            charChart.occurence -= 1

            if(charChart.occurence < charChart.maxOccurence) {
                charChart.image.isEnabled = true
                charChart.image.alpha = 1f
            }
        }

        selectedCharacterInGame = null
        setSelectionButtonEnable(false)
    }

    private fun previousOrder() {
        val index : Int = selectedCharacterInGame!!.order

        //We must do all the process in the same time. Cutting in two result in
        //the list to return to its initial statement since filter return another list
        charactersSelected.forEach {  x ->
            if(x.order == index - 1)
                x.order += 1
            else if(x.order == index)
                x.order -= 1
        }

    }

    private fun nextOrder() {
        val index : Int = selectedCharacterInGame!!.order

        charactersSelected.forEach {  x ->
            if(x.order == index + 1)
                x.order -= 1
            else if(x.order == index)
                x.order += 1
        }


    }

    private fun removeSelectedCharacterInGame() {

        selectedCharacterInGame = null

        //We only refresh view when canceling
        refreshSelectedListCharacters()

    }

    private fun refreshArrowSelectionSelected() {
        if(selectedCharacterInGame != null) {
            binding.leftArrowButton.isEnabled = selectedCharacterInGame!!.order != 0
            binding.rightArrowButton.isEnabled = selectedCharacterInGame!!.order != charactersSelected.size - 1
        }

    }

    private fun setButtonListenersOfSelected() {
        binding.startButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            val gson = Gson()

            try {
                val strList = gson.toJson(charactersSelected)

                intent.putExtra("GameList", strList)

                startActivity(intent)
            } catch(ex : Exception) {
                println(ex)
            }

        }

        binding.leftArrowButton.setOnClickListener {
            previousOrder()
            refreshSelectedListCharacters()
        }

        binding.rightArrowButton.setOnClickListener {
            nextOrder()
            refreshSelectedListCharacters()
        }

        binding.cancelButton.setOnClickListener {
            removeSelectedCharacterInGame()
        }

        binding.removeButton.setOnClickListener {
            removeCharacterToSelection()
            refreshSelectedListCharacters()
        }
    }
}