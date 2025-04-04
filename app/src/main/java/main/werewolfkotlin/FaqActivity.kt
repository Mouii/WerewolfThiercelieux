package main.werewolfkotlin

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityFaqBinding

class FaqActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityFaqBinding

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        for(i in 1..16) {

            val pair : Pair<Int, Int>? = getQuestionAnswer(i)

            if(pair != null) {

                val textAsk = TextView(this).apply {
                    text = getString(pair.first)
                    setTextColor(Color.WHITE)
                    textSize = 20f
                }

                binding.gridLayout.addView(textAsk)

                val textAns = TextView(this).apply {
                    text = getString(pair.second)
                    setTextColor(Color.WHITE)
                    textSize = 15f
                }

                binding.gridLayout.addView(textAns)
            }
        }

        binding.returnButton.setOnClickListener {
            finish()
        }
    }


    private fun getQuestionAnswer(number : Int) : Pair<Int, Int>? {

        when(number) {
            1 -> return Pair(R.string.FaqView_question1, R.string.FaqView_answer1)
            2 -> return Pair(R.string.FaqView_question2, R.string.FaqView_answer2)
            3 -> return Pair(R.string.FaqView_question3, R.string.FaqView_answer3)
            4 -> return Pair(R.string.FaqView_question4, R.string.FaqView_answer4)
            5 -> return Pair(R.string.FaqView_question5, R.string.FaqView_answer5)
            6 -> return Pair(R.string.FaqView_question6, R.string.FaqView_answer6)
            7 -> return Pair(R.string.FaqView_question7, R.string.FaqView_answer7)
            8 -> return Pair(R.string.FaqView_question8, R.string.FaqView_answer8)
            9 -> return Pair(R.string.FaqView_question9, R.string.FaqView_answer9)
            10 -> return Pair(R.string.FaqView_question10, R.string.FaqView_answer10)
            11 -> return Pair(R.string.FaqView_question11, R.string.FaqView_answer11)
            12 -> return Pair(R.string.FaqView_question12, R.string.FaqView_answer12)
            13 -> return Pair(R.string.FaqView_question13, R.string.FaqView_answer13)
            14 -> return Pair(R.string.FaqView_question14, R.string.FaqView_answer14)
            15 -> return Pair(R.string.FaqView_question15, R.string.FaqView_answer15)
            16 -> return Pair(R.string.FaqView_question16, R.string.FaqView_answer16)
        }

        return null
    }



}