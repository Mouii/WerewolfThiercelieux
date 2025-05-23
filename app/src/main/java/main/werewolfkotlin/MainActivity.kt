package main.werewolfkotlin

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityMainBinding
import java.lang.System.exit
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    /***
     * Spinner adapter for languages
     */
    private class SpinnerAdapter(context: Context, items: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.gravity = android.view.Gravity.CENTER // Center the text
            view.setTextColor(Color.BLACK)
            view.setBackgroundColor(Color.WHITE)
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.gravity = android.view.Gravity.CENTER // Center dropdown items' text
            view.setTextColor(Color.BLACK)
            view.setBackgroundColor(Color.WHITE)
            return view
        }
    }

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply locale before setting the content view
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        var selectedLanguage = sharedPreferences.getString("language", null)

        if(selectedLanguage == null) {
            selectedLanguage = Locale.getDefault().language

            // Save the selected language in SharedPreferences
            sharedPreferences.edit().putString("language", selectedLanguage).apply()
        }

        val updatedContext = updateLocal(selectedLanguage!!, this)

        // Apply new configuration to the app resources
        resources.updateConfiguration(updatedContext.resources.configuration, updatedContext.resources.displayMetrics)


        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if(WorkerEasier.characterListType.isEmpty()) {

            //If fail first time => not created
            if(!loadCharacterFromJson()) {
                WorkerEasier.resetCharacters(this)
            }
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
            exitProcess(0)

        }

        // Retrieve saved selection
        var savedPosition = sharedPreferences.getInt("spinner_position", -1) // Default to position 0

        //if no saved position, only apply on first instance of app
        if(savedPosition == -1) {
            //Affect the local
            savedPosition = WorkerEasier.listLang.indexOf(selectedLanguage)

            //Not handled langage => english
            if(savedPosition == -1)
                savedPosition = 0
        }

        //To put the default position correctly before
        binding.langSpinner.onItemSelectedListener = null

        //Re-put the list
        binding.langSpinner.adapter = SpinnerAdapter(this, WorkerEasier.listLang)

        //Set the saved position
        binding.langSpinner.setSelection(savedPosition)

        //Put back listener
        binding.langSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if(position != savedPosition) {
                        // Retrieve the selected item
                        val selectedItem = parent?.getItemAtPosition(position).toString()

                        // Save the selected language in SharedPreferences
                        sharedPreferences.edit().putString("language", selectedItem).apply()

                        // Save the selection
                        sharedPreferences.edit().putInt("spinner_position", position).apply()

                        //Reset for new import
                        WorkerEasier.resetForTranslation()

                        recreate()
                    }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

    }

    /***
     * Change the language of the application
     * lang : String
     */
    private fun updateLocal(lang : String, context : Context) : Context {

        //Change local
        val locale = Locale(lang)
        Locale.setDefault(locale)

        //Change configuration
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        //Reset important values for restarting
        WorkerEasier.resetDataSaved()

        // Create and return a new context with the updated locale
        return context.createConfigurationContext(config)

    }

    /***
     * Load list of characters from the json file
     */
    private fun loadCharacterFromJson() : Boolean {
        try {
            WorkerEasier.setCharactersFromJson(this.filesDir.toString().plus("/json/Roles.json"), this)
            return true
        } catch (ex : Exception) {
            println(ex)
            return false
        }
    }

}