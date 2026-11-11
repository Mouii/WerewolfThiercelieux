package main.werewolfkotlin

import android.graphics.Color
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
import main.werewolfkotlin.databinding.ActivityRoleBinding
import model.CharacterGame
import model.PowerState

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
        val alive = members.filter { c -> characterGames.any { it === c } }

        val band = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
        }

        //Header, always visible
        val title = if (members.size > 1) "${first.name} ×${members.size}" else first.name
        val aliveText = getString(R.string.RoleView_Alive, alive.size, members.size)
        val header = TextView(this).apply {
            text = "▸  $title  ·  $aliveText"
            setTextColor(Color.WHITE)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#444444"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            if (alive.isEmpty()) alpha = 0.5f
        }
        band.addView(header)

        //Body, shown when the band is tapped
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#3A3A3A"))
            setPadding(dp(14), dp(10), dp(14), dp(14))
            visibility = View.GONE
        }

        val picture = ImageView(this).apply {
            val res = WorkerEasier.getCharacterImage(first)
            if (res != 0) setImageResource(res)
            adjustViewBounds = true
        }
        body.addView(picture, LinearLayout.LayoutParams(dp(180), dp(180)))

        val description = TextView(this).apply {
            text = first.description
            setTextColor(Color.WHITE)
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
            setTextColor(Color.WHITE)
            isEnabled = alive.isNotEmpty()
            isChecked = alive.isNotEmpty() && GameState.shownAwake(alive.first())
            setOnCheckedChangeListener { _, checked ->
                GameState.setAwakeGroup(this@RoleActivity, alive, checked, first.name)
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
                            removeVillagersPowers()
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

    /***
     * The special villagers (not solo, not werewolves, the infected don't count) lose their powers
     */
    private fun removeVillagersPowers() {
        characterGames.filter { !it.isSolo && !it.isWerewolf }.forEach { x ->
            x.isNocturnal = false
            x.powerState = PowerState.PERMANENT
        }
        GameState.eldersPowerRemoved = true
        GameState.log(this, R.string.History_Elder)
    }
}
