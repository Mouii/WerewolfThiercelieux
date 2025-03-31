package main.werewolfkotlin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import main.werewolfkotlin.databinding.ActivityInformationBinding
import main.werewolfkotlin.databinding.ActivityRoleBinding

class RoleActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityRoleBinding

    ///
    /// Execution on creation of the activity
    ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Listener on button
        binding.returnButton.setOnClickListener {
            //In order to transfer the list to the other activity, we create an intent
            //in direction of the game activity
            val intent = Intent(this, GameActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }

            //Start the other activity
            startActivity(intent)
        }

        if(roleListConsumable.isNotEmpty()) {

            roleListConsumable.forEach { characterMap ->
                run {

                    val switch = SwitchCompat(this).apply {
                        text = characterMap.key.className
                        isChecked = roleListConsumable[characterMap.key]!!
                        isEnabled = !roleListConsumable[characterMap.key]!!
                        setTextColor(Color.WHITE)
                        switchPadding = 5
                    }

                    switch.setOnClickListener {

                        characterMap.key.isNocturnal = !characterMap.key.isNocturnal
                        roleListConsumable[characterMap.key] = true

                        switch.isEnabled = false
                    }

                    binding.gridLayout.addView(switch)
                }
            }
        }
    }

}