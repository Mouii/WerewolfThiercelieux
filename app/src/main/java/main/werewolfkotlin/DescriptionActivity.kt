package main.werewolfkotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.json.Json
import main.werewolfkotlin.databinding.ActivityDescriptionBinding

class DescriptionActivity: AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityDescriptionBinding

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.returnButton.setOnClickListener {
            finish()
        }
    }
}