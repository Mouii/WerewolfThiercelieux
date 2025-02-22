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


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val charactersMenu : MutableList<Character> = mutableListOf()

    private val charactersSelected : MutableList<Character> = mutableListOf()

    private val imageDimension : Int = 240
    private val imageMargin : Int = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        charactersMenu.add(Werewolf(4, R.drawable.werewolf))
        charactersMenu.add(Werewolf(4, R.drawable.werewolf))
        charactersMenu.add(Werewolf(4, R.drawable.werewolf))
        charactersMenu.add(Werewolf(4, R.drawable.werewolf))
        charactersMenu.add(Werewolf(4, R.drawable.werewolf))
        charactersMenu.add(Seer(3, R.drawable.seer))
        charactersMenu.add(Witch(5, R.drawable.witch))
        charactersMenu.add(Hunter(98, R.drawable.hunter))
        charactersMenu.add(Thief(1, R.drawable.thief))
        charactersMenu.add(Cupid(2, R.drawable.cupid))
        charactersMenu.add(LittleGirl(4, R.drawable.littlegirl))
        charactersMenu.add(Villager(99, R.drawable.villager))
        charactersMenu.add(Villager(99, R.drawable.villager))
        charactersMenu.add(Villager(99, R.drawable.villager))
        charactersMenu.add(Villager(99, R.drawable.villager))
        charactersMenu.add(Villager(99, R.drawable.villager))

        charactersMenu.sortedBy { x -> x.order }

        charactersSelected.add(Werewolf(4, R.drawable.werewolf))
        charactersSelected.add(Werewolf(4, R.drawable.werewolf))
        charactersSelected.add(Werewolf(4, R.drawable.werewolf))
        charactersSelected.add(Werewolf(4, R.drawable.werewolf))
        charactersSelected.add(Werewolf(4, R.drawable.werewolf))
        charactersSelected.add(Seer(3, R.drawable.seer))
        charactersSelected.add(Witch(5, R.drawable.witch))
        charactersSelected.add(Hunter(98, R.drawable.hunter))
        charactersSelected.add(Thief(1, R.drawable.thief))
        charactersSelected.add(Cupid(2, R.drawable.cupid))
        charactersSelected.add(LittleGirl(4, R.drawable.littlegirl))
        charactersSelected.add(Villager(99, R.drawable.villager))
        charactersSelected.add(Villager(99, R.drawable.villager))
        charactersSelected.add(Villager(99, R.drawable.villager))
        charactersSelected.add(Villager(99, R.drawable.villager))
        charactersSelected.add(Villager(99, R.drawable.villager))

        charactersSelected.sortedBy { x -> x.order }


        var index: Int = 0

        for (character in charactersMenu) {
            val imageView : ImageView = setImagePicture(character.order, character)
            binding.gridSelectView.addView(imageView, index)
            index++
        }

        index = 0

        for (character in charactersSelected) {
            val imageView : ImageView = setImagePicture(character.order, character)
            binding.gridSelectedView.addView(imageView, index)
            index++
        }

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


}