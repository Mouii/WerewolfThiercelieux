package main.werewolfkotlin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.json.Json
import main.werewolfkotlin.databinding.ActivityMainBinding
import model.WorkerEasier

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val jsonString = assets.open("Roles.json").bufferedReader().use { it.readText() }
            WorkerEasier.setCharactersFromJson(jsonString)

        } catch (ex : Exception) {
            println(ex)
        }

        binding.selectButton.setOnClickListener {

            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, SelectActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }

        binding.roleButton.setOnClickListener {

            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, DescriptionActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }

        binding.faqButton.setOnClickListener {

            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, FaqActivity::class.java)

            //Start the other activity
            startActivity(intent)
        }

        binding.exitButton.setOnClickListener {

            //Finish linked activities
            finishAffinity()

            //Close actual activity
            finish()

        }
    }
}