package main.werewolfkotlin

import Model.*
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
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

        charactersSelected.sortBy { x -> x.order }

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

    private fun drawSelection(imageView: ImageView) {
        for(image in binding.gridSelectedView) {
            if(image == imageView) {
                image.setPadding(10, 10, 10, 10)
                image.setBackgroundColor(Color.GREEN)
            } else {
                image.setPadding(0, 0, 0, 0)
                image.setBackgroundColor(Color.TRANSPARENT)
            }
        }

    }

    private fun refreshSelectedListCharacters() {
        binding.gridSelectedView.removeAllViewsInLayout()

        charactersSelected.sortBy{ x -> x.order }

        for ((index, character) in charactersSelected.withIndex()) {
            val imageView : ImageView = setImagePicture(character)
            binding.gridSelectedView.addView(imageView, index)

            imageView.setOnClickListener {
                if(selectedCharacterInGame != character) {
                    selectedCharacterInGame = character
                    setSelectionButtonEnable(true)
                    drawSelection(imageView)
                } else {
                    setSelectionButtonEnable(false)
                    removeSelectedCharacterInGame()
                }

                refreshArrowSelectionSelected()
            }

            if(selectedCharacterInGame == character) {
                drawSelection(imageView)
            }

        }

        refreshArrowSelectionSelected()
    }

    private fun addCharacterToGame(character: Character) {
        val newCharacter : Character = character.clone()
        newCharacter.order = charactersSelected.size
        charactersSelected.add(newCharacter)
    }

    private fun removeCharacterToSelection() {
        if(selectedCharacterInGame != null) {
            charactersSelected.remove(selectedCharacterInGame);

            val charChart = characterChart.first{ x -> x.name == selectedCharacterInGame!!.className}
            charChart.occurence -= 1

            if(charChart.occurence < charChart.maxOccurence) {
                charChart.image.isEnabled = true
            }
        }


    }

    private fun previousOrder() {
        val index : Int = charactersSelected.indexOf(selectedCharacterInGame)

        charactersSelected[index-1].order += 1
        charactersSelected[index].order -= 1

    }

    private fun nextOrder() {
        val index : Int = charactersSelected.indexOf(selectedCharacterInGame)

        charactersSelected[index+1].order -= 1
        charactersSelected[index].order += 1

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