package main.werewolfkotlin.ui.activity

import main.werewolfkotlin.ui.design.colorOf

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import main.werewolfkotlin.R
import main.werewolfkotlin.databinding.ActivityRoleBinding
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.NightFlow
import main.werewolfkotlin.game.PowerRules
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.dp
import model.CharacterGame
import model.kindOf

/***
 * Roles of the game, dead or alive. Each role is a band : tap it to see its picture and its description.
 */
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

        //Listener on button
        binding.returnButton.setOnClickListener {
            finish()
        }

        buildBands()
    }

    private fun buildBands() {
        binding.roleContainer.removeAllViews()

        val groups = GameState.allCharacters.sortedBy { it.order }.groupBy { kindOf(it) }.values
        for (members in groups) {
            binding.roleContainer.addView(buildBand(members))
        }
    }

    /***
     * One band for a kind of role (all the players having it)
     */
    private fun buildBand(members: List<CharacterGame>): View {
        val first = members.first()
        val alive = members.filter { c -> GameState.aliveCharacters.any { it === c } }

        val band = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
        }

        //Header, always visible
        val title = first.name
        val aliveText = getString(R.string.RoleView_Alive, alive.size, members.size)
        val header = TextView(this).apply {
            text = "▸  $title  ·  $aliveText"
            setTextColor(colorOf(R.color.white))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundColor(colorOf(R.color.card))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            if (alive.isEmpty()) alpha = 0.5f
        }
        band.addView(header)

        //Body, shown when the band is tapped
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(colorOf(R.color.card_row))
            setPadding(dp(14), dp(10), dp(14), dp(14))
            visibility = View.GONE
        }

        val picture = ImageView(this).apply {
            val res = RoleImages.of(first)
            if (res != 0) setImageResource(res)
            adjustViewBounds = true
        }
        body.addView(picture, LinearLayout.LayoutParams(dp(180), dp(180)))

        val description = TextView(this).apply {
            text = first.description
            setTextColor(colorOf(R.color.white))
            textSize = 15f
            setPadding(0, dp(10), 0, dp(10))
        }
        body.addView(description, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        //Wake up / put to sleep, in case of a mistake
        val awakeSwitch = SwitchCompat(this).apply {
            text = getString(R.string.Band_Awake)
            setTextColor(colorOf(R.color.white))
            isEnabled = alive.isNotEmpty()
            isChecked = alive.isNotEmpty() && GameState.shownAwake(alive.first())
            setOnCheckedChangeListener { _, checked ->
                NightFlow.setAwakeGroup(alive, checked, first.name)
            }
        }
        body.addView(awakeSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        //Special button of the elder : the powers of the special villagers are removed
        if (first.className == "Elder") {
            val elderButton = MaterialButton(this).apply {
                setText(R.string.RoleView_AncientButton)
                isEnabled = !GameState.eldersPowerRemoved
                setOnClickListener {
                    AlertDialog.Builder(this@RoleActivity)
                        .setTitle(getString(R.string.RoleView_DialogTitle))
                        .setMessage(getString(R.string.RoleView_DialogDescription))
                        .setPositiveButton(getString(R.string.Generic_Yes)) { _, _ ->
                            PowerRules.removeVillagersPowers()
                            isEnabled = false
                        }
                        .setNegativeButton(getString(R.string.Generic_Cancel), null)
                        .show()
                }
            }
            body.addView(elderButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) })
        }

        band.addView(body)

        header.setOnClickListener {
            val open = body.visibility != View.VISIBLE
            body.visibility = if (open) View.VISIBLE else View.GONE
            header.text = (if (open) "▾  " else "▸  ") + title + "  ·  " + aliveText
        }

        return band
    }
}
