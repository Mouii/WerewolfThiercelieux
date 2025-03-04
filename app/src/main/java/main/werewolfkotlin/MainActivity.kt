package main.werewolfkotlin

import Model.*
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.iterator
import androidx.room.util.copy
import main.werewolfkotlin.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val charactersMenu : MutableList<Character> = mutableListOf()

    private val charactersSelected : MutableList<Character> = mutableListOf()

    private val imageDimension : Int = 240
    private val imageMargin : Int = 10

    private var selectedCharacterInGame: Character? = null

    private val characterChart : MutableList<CharacterChart> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var villager = Villager(1, R.drawable.villager)
        var werewolf = Werewolf(2, R.drawable.werewolf)
        var seer = Seer(3, R.drawable.seer)
        var witch = Witch(4, R.drawable.witch)
        var hunter = Hunter(5, R.drawable.hunter)
        var littleGirl = LittleGirl(6, R.drawable.littlegirl)
        var thief = Thief(7, R.drawable.thief)
        var cupid = Cupid(8, R.drawable.cupid)

        charactersMenu.add(villager)
        charactersMenu.add(werewolf)
        charactersMenu.add(seer)
        charactersMenu.add(witch)
        charactersMenu.add(hunter)
        charactersMenu.add(littleGirl)
        charactersMenu.add(thief)
        charactersMenu.add(cupid)

        charactersMenu.sortedBy { x -> x.order }

        charactersSelected.sortedBy { x -> x.order }

        for ((index, character) in charactersMenu.withIndex()) {


            val imageView : ImageView = setImagePicture(character.order, character)

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

    private fun setImagePicture(index: Int, character : Character): ImageView {
        val imageView = ImageView(this).apply {
            setImageResource(character.imageResource)
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
        var index: Int = 0

        for (character in charactersSelected.sortedBy{x -> x.order }) {
            val imageView : ImageView = setImagePicture(character.order, character)
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

            index++
        }
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

        //Remove for one action at end time
        setSelectionButtonEnable(false)
        removeSelectedCharacterInGame()

    }

    private fun nextOrder() {
        val index : Int = charactersSelected.indexOf(selectedCharacterInGame)

        charactersSelected[index+1].order -= 1
        charactersSelected[index].order += 1

        //Remove for one action at end time
        setSelectionButtonEnable(false)
        removeSelectedCharacterInGame()
    }

    private fun removeSelectedCharacterInGame() {
        //We only refresh view when canceling
        refreshSelectedListCharacters()
        selectedCharacterInGame = null
    }

    private fun refreshArrowSelectionSelected() {
        if(selectedCharacterInGame != null) {
            binding.leftArrowButton.isEnabled = selectedCharacterInGame!!.order != 0
            binding.rightArrowButton.isEnabled = selectedCharacterInGame!!.order != charactersSelected.size - 1
        }

    }

    private fun setButtonListenersOfSelected() {
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