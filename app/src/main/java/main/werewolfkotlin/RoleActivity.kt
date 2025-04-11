package main.werewolfkotlin

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
            finish()
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