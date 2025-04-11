package main.werewolfkotlin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //If fail first time => not created
        if(!loadCharacterFromJson()) {
            WorkerEasier.resetCharacters(this)
            loadCharacterFromJson()
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

    private fun loadCharacterFromJson() : Boolean {
        try {
            val jsonString = File(this.filesDir.toString().plus("/json"), "Roles.json").bufferedReader().use { it.readText() }
            WorkerEasier.setCharactersFromJson(jsonString)
            return true
        } catch (ex : Exception) {
            println(ex)
            return false
        }
    }

}