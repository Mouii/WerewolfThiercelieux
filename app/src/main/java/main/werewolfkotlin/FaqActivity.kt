package main.werewolfkotlin

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityFaqBinding

class FaqActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityFaqBinding

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var index = 1

        var pair : Pair<String, String>? = null

        do {

            pair = getQuestionAnswer(index)

            if(pair != null) {

                val textAsk = TextView(this).apply {
                    text = pair.first
                    setTextColor(Color.WHITE)
                    textSize = 20f
                }

                binding.gridLayout.addView(textAsk)

                val textAns = TextView(this).apply {
                    text = pair.second
                    setTextColor(Color.WHITE)
                    textSize = 15f
                }

                binding.gridLayout.addView(textAns)
            }

            index++
        } while(pair != null)

        binding.returnButton.setOnClickListener {
            finish()
        }
    }

    /***
     * Return the associate string for the complete FAQ
     */
    private fun getQuestionAnswer(number : Int) : Pair<String, String>? {

        val question = WorkerEasier.getStringByKey("FaqView_question".plus(number), this)
        val answer = WorkerEasier.getStringByKey("FaqView_answer".plus(number), this)

        if(question == "" || answer == "")
            return null

        return Pair(question, answer)
    }

}