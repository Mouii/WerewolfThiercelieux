package main.werewolfkotlin

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.forEach
import main.werewolfkotlin.databinding.ActivityRoleBinding
import model.Elder
import model.PowerState

class RoleActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityRoleBinding

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ancientButton.setOnClickListener {
            // Create the dialog
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.RoleView_DialogTitle))
                .setMessage(getString(R.string.RoleView_DialogDescription))
                .setPositiveButton(getString(R.string.Generic_Yes)) { _, _->
                    characterGames.filter { !it.isSolo && !it.isWerewolf }.forEach { x ->
                        x.isNocturnal = false
                        x.powerState = PowerState.PERMANENT
                        roleListConsumable[x] = true
                        binding.ancientButton.isEnabled = false
                    }
                    binding.gridLayout.forEach { x ->
                        x.isEnabled = false
                    }
                }
                .setNegativeButton(getString(R.string.Generic_Cancel), null)
                .show()
        }

        //Listener on button
        binding.returnButton.setOnClickListener {
            finish()
        }

        if(roleListConsumable.isNotEmpty()) {

            roleListConsumable.forEach { characterMap ->
                run {

                    val switch = SwitchCompat(this).apply {
                        text = characterMap.key.name
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