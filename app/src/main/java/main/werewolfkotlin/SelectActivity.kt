package main.werewolfkotlin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import main.werewolfkotlin.databinding.ActivitySelectBinding
import model.CharacterGame

/***
 * Game setup : the extensions that are used, the number of players and their names.
 * The roles are not chosen here : they are given to the players on the game screen.
 */
class SelectActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivitySelectBinding

    private val minPlayers = 4
    private val maxPlayers = 46

    private var count = 8
    private val nameFields = mutableListOf<EditText>()

    //Extensions whose bar is open
    private val openExtensions = mutableSetOf<Extension>()

    private lateinit var extensionsBox: LinearLayout
    private lateinit var resumeButton: View
    private lateinit var countText: TextView
    private lateinit var namesBox: LinearLayout

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        count = PlayerNames.readCount(this).coerceIn(minPlayers, maxPlayers)

        //A game that was left can be resumed
        resumeButton = panelButton(this, getString(R.string.Select_Resume), Color.parseColor("#00E676")).apply {
            setOnClickListener {
                startActivity(Intent(this@SelectActivity, GameActivity::class.java).putExtra("Resume", true))
            }
        }
        binding.setupContainer.addView(resumeButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(12)) })

        buildExtensionsCard()
        buildPlayersCard()

        binding.backButton.setOnClickListener { finish() }
        binding.startButton.setOnClickListener { startGame() }
    }

    /***
     * The roles of the extensions may have been moved on the configuration screen
     */
    override fun onPause() {
        super.onPause()
        saveVisibleNames()
    }

    /***
     * Keep what is written, the names are shared with the game
     */
    private fun saveVisibleNames() {
        if (nameFields.isEmpty()) return
        val saved = PlayerNames.readAll(this).toMutableList()
        nameFields.forEachIndexed { i, field ->
            while (saved.size <= i) saved.add("")
            saved[i] = field.text.toString().trim()
        }
        PlayerNames.saveAll(this, saved)
    }

    override fun onResume() {
        super.onResume()

        //A name changed during the game shows here
        val saved = PlayerNames.readAll(this)
        nameFields.forEachIndexed { i, field ->
            val name = saved.getOrNull(i) ?: ""
            if (field.text.toString() != name) field.setText(name)
        }
        if (::extensionsBox.isInitialized) fillExtensions()
        if (::resumeButton.isInitialized) resumeButton.visibility = if (GameSave.exists(this)) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────────────────────────
    //  Extensions
    // ──────────────────────────────────────────────────────────────

    private fun card(title: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = PanelDialog.panelBackground(this@SelectActivity)
            setPadding(dp(14), dp(12), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(12)) }
        }
        card.addView(TextView(this).apply {
            text = title
            setTextColor(PanelDialog.CYAN)
            textSize = 13f
            letterSpacing = 0.1f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        binding.setupContainer.addView(card)
        return card
    }

    private fun buildExtensionsCard() {
        val card = card(getString(R.string.Select_Extensions))

        extensionsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(extensionsBox)

        fillExtensions()
    }

    private fun fillExtensions() {
        extensionsBox.removeAllViews()

        for (extension in Extension.values()) {
            //Roles of this extension
            val roles = WorkerEasier.characterListType.filter { Extensions.extensionOf(this, it.first) == extension }
            val enabled = Extensions.isEnabled(this, extension)

            //Cards of the roles, 4 per line
            val body = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (extension in openExtensions) View.VISIBLE else View.GONE
                alpha = if (enabled) 1f else 0.4f
                setPadding(0, dp(6), 0, dp(4))
            }
            for (line in roles.chunked(4)) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (i in 0 until 4) {
                    val cell : View = if (i < line.size) {
                        val character = line[i].second.values.first()
                        val picture = WorkerEasier.getCharacterImage(character)
                        if (picture != 0) {
                            ImageView(this).apply {
                                setImageResource(picture)
                                adjustViewBounds = true
                                contentDescription = character.name
                            }
                        } else {
                            TextView(this).apply {
                                text = character.name
                                setTextColor(Color.WHITE)
                                gravity = Gravity.CENTER
                                textSize = 11f
                            }
                        }
                    } else {
                        View(this)
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), dp(3), dp(3), dp(3))
                    })
                }
                body.addView(row)
            }

            //Bar : arrow, name of the extension, button to activate it
            val arrow = TextView(this).apply {
                text = if (extension in openExtensions) "▾" else "▸"
                setTextColor(PanelDialog.CYAN)
                textSize = 16f
            }
            val bar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = GradientDrawable().apply {
                    setColor(Color.argb(60, 0, 229, 255))
                    setStroke(dp(1), PanelDialog.CYAN)
                    cornerRadius = dp(10).toFloat()
                }
                isClickable = true
                setOnClickListener {
                    val open = body.visibility != View.VISIBLE
                    body.visibility = if (open) View.VISIBLE else View.GONE
                    arrow.text = if (open) "▾" else "▸"
                    if (open) openExtensions.add(extension) else openExtensions.remove(extension)
                }
            }
            bar.addView(arrow, LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT))
            bar.addView(TextView(this).apply {
                text = getString(extension.nameRes) + "  (" + roles.size + ")"
                setTextColor(Color.WHITE)
                textSize = 16f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            bar.addView(SwitchCompat(this).apply {
                isChecked = enabled
                setOnCheckedChangeListener { _, checked ->
                    Extensions.setEnabled(this@SelectActivity, extension, checked)
                    body.alpha = if (checked) 1f else 0.4f
                }
            })

            extensionsBox.addView(bar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) })
            extensionsBox.addView(body)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Players
    // ──────────────────────────────────────────────────────────────

    private fun roundButton(label: String, onClick: () -> Unit) = panelButton(this, label).apply {
        minWidth = 0
        minimumWidth = dp(48)
        setOnClickListener { onClick() }
    }

    private fun buildPlayersCard() {
        val card = card(getString(R.string.Select_Players))

        //Number of players
        val countRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        countRow.addView(roundButton("−") { changeCount(count - 1) })
        countText = TextView(this).apply {
            text = count.toString()
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        countRow.addView(countText, LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.WRAP_CONTENT))
        countRow.addView(roundButton("+") { changeCount(count + 1) })
        card.addView(countRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(6)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        //Forget the names
        card.addView(panelButton(this, getString(R.string.Select_ClearNames)).apply {
            setOnClickListener {
                nameFields.forEach { it.setText("") }
                PlayerNames.saveAll(this@SelectActivity, emptyList())
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        //Names, one line per player
        namesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(namesBox)

        val saved = PlayerNames.readAll(this)
        for (i in 0 until count) addNameRow(i, saved.getOrNull(i) ?: "")
    }

    private fun addNameRow(index: Int, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

        row.addView(TextView(this).apply {
            text = (index + 1).toString() + "."
            setTextColor(Color.parseColor("#8FA3B8"))
            textSize = 15f
        }, LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT))

        val field = EditText(this).apply {
            setText(value)
            hint = getString(R.string.SeatDefaultName, index + 1)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#667788"))
        }
        row.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        nameFields.add(field)
        namesBox.addView(row)
    }

    private fun changeCount(newCount: Int) {
        val wanted = newCount.coerceIn(minPlayers, maxPlayers)
        if (wanted == count) return

        //The names already written stay
        val saved = PlayerNames.readAll(this)
        while (nameFields.size < wanted) addNameRow(nameFields.size, saved.getOrNull(nameFields.size) ?: "")
        while (nameFields.size > wanted) {
            nameFields.removeAt(nameFields.size - 1)
            namesBox.removeViewAt(namesBox.childCount - 1)
        }

        count = wanted
        countText.text = count.toString()
    }

    // ──────────────────────────────────────────────────────────────
    //  Start
    // ──────────────────────────────────────────────────────────────

    private fun startGame() {
        if (Extension.values().none { Extensions.isEnabled(this, it) }) {
            Toast.makeText(this, R.string.Select_NoExtension, Toast.LENGTH_SHORT).show()
            return
        }

        //The names are kept for the next games (the names of the players that don't exist now are kept too)
        val names = nameFields.map { it.text.toString().trim() }
        val saved = PlayerNames.readAll(this).toMutableList()
        names.forEachIndexed { i, name ->
            while (saved.size <= i) saved.add("")
            saved[i] = name
        }
        PlayerNames.saveAll(this, saved)
        PlayerNames.saveCount(this, count)

        //A new game replaces the one that was left
        GameSave.clear(this)

        val intent = Intent(this, GameActivity::class.java)
        intent.putStringArrayListExtra("PlayerNames", ArrayList(names))
        startActivity(intent)
    }
}
