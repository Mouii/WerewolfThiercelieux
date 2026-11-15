package main.werewolfkotlin.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.data.AppSettings
import main.werewolfkotlin.data.RolesRepository
import main.werewolfkotlin.databinding.ActivityMainBinding
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.adapter.SpinnerAdapter
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply locale before setting the content view
        val selectedLanguage = AppSettings.language(this) ?: Locale.getDefault().language.also {
            // Save the selected language in SharedPreferences
            AppSettings.setLanguage(this, it)
        }

        val updatedContext = updateLocal(selectedLanguage, this)

        // Apply new configuration to the app resources
        resources.updateConfiguration(updatedContext.resources.configuration, updatedContext.resources.displayMetrics)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (!RolesRepository.isLoaded) {
            //If fail first time => not created
            if (!RolesRepository.loadFromPhone(this)) {
                RolesRepository.resetCharacters(this)
            }
        }

        binding.selectButton.setOnClickListener { open(SelectActivity::class.java) }
        binding.roleButton.setOnClickListener { open(DescriptionActivity::class.java) }
        binding.faqButton.setOnClickListener { open(FaqActivity::class.java) }

        binding.exitButton.setOnClickListener {

            //Finish linked activities
            finishAffinity()

            //Close actual activity
            exitProcess(0)

        }

        // Retrieve saved selection
        var savedPosition = AppSettings.spinnerPosition(this)

        //if no saved position, only apply on first instance of app
        if (savedPosition == -1) {
            //Affect the local
            savedPosition = AppSettings.languages.indexOf(selectedLanguage)

            //Not handled langage => english
            if (savedPosition == -1)
                savedPosition = 0
        }

        //To put the default position correctly before
        binding.langSpinner.onItemSelectedListener = null

        //Re-put the list
        binding.langSpinner.adapter = SpinnerAdapter(this, AppSettings.languages)

        //Set the saved position
        binding.langSpinner.setSelection(savedPosition)

        //Put back listener
        binding.langSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != savedPosition) {
                    // Save the selected language and its position
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    AppSettings.setLanguageChoice(this@MainActivity, selectedItem, position)

                    //Reset for new import
                    RolesRepository.resetForTranslation()
                    Labels.clear()

                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle the case where no item is selected
            }
        }

    }

    /***
     * Open another screen
     */
    private fun open(screen: Class<*>) {
        startActivity(Intent(this, screen))
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
        RolesRepository.resetDataSaved()

        // Create and return a new context with the updated locale
        return context.createConfigurationContext(config)

    }
}
