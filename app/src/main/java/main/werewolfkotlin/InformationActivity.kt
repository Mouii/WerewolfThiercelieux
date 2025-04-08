package main.werewolfkotlin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityInformationBinding

class InformationActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityInformationBinding

    private fun createSharedTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                // Get the current EditText using binding's focused child (or track focus manually)
                val currentEditText = currentFocus as? EditText

                addToText(currentEditText!!.id, currentEditText.text.toString())
            }
        }
    }



    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*binding.gridLayout.setOnTouchListener(object : OnSwipeTouchListener(this@InformationActivity) {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onSwipeRight() {
                sweepRight()
            }
        })*/

        //Listener on button
        binding.returnButton.setOnClickListener {
            finish()
        }

        //Object watcher
        val sharedTextWatcher = createSharedTextWatcher()

        //List of editText
        val listEdit = listOf(
            binding.deathEdit
            ,binding.loverEdit
            ,binding.defenderEdit
            ,binding.wildEdit
            ,binding.bearEdit
            ,binding.ancientEdit
            ,binding.infectedEdit
            ,binding.sectarianEdit
            ,binding.comedianEdit
            ,binding.thiefEdit
            ,binding.piperEdit
            ,binding.scapegoatEdit
            ,binding.switchEdit
            ,binding.punishedEdit
        )

        //Affectation
        listEdit.forEach { editText ->

            //Must be put before, otherwise crash application if listener is put
            if(infoMap.containsKey(editText.id)) {
                editText.setText(infoMap[editText.id])
            }

            editText.addTextChangedListener(sharedTextWatcher)
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                    true
                } else false
            }

            editText.imeOptions = EditorInfo.IME_ACTION_NONE
        }

    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sweepRight() {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, GameActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        //Start the other activity
        startActivity(intent)
    }


    private fun addToText(key: Int, value: String){
        infoMap[key] = value
    }


}