package main.werewolfkotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import main.werewolfkotlin.databinding.ActivityInformationBinding

class InformationActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityInformationBinding


    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*binding.mainLayout.setOnTouchListener(object : OnSwipeTouchListener(this@InformationActivity) {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onSwipeRight() {
                sweepRight()
            }
        })*/
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sweepRight() {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, InformationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        //Start the other activity
        startActivity(intent)
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_from_left,
            R.anim.slide_out_to_right)
    }
}