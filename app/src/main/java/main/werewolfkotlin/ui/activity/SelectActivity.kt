package main.werewolfkotlin.ui.activity

import main.werewolfkotlin.ui.design.colorOf

import main.werewolfkotlin.R
import main.werewolfkotlin.data.ExtensionSettings
import main.werewolfkotlin.data.GameSaveStore
import main.werewolfkotlin.data.PlayerNamesStore
import main.werewolfkotlin.data.RolesRepository
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.dp
import main.werewolfkotlin.ui.design.panelBackground
import main.werewolfkotlin.ui.design.roundBg
import main.werewolfkotlin.ui.dialog.panelButton
import model.Extension

import android.content.Context
import android.content.Intent
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

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        count = PlayerNamesStore.readCount(this).coerceIn(minPlayers, maxPlayers)

        //A game that was left can be resumed
        resumeButton = panelButton(this, getString(R.string.Select_Resume), colorOf(R.color.ok)).apply {
            setOnClickListener {
                startActivity(Intent(this@SelectActivity, GameActivity::class.java).putExtra(GameActivity.EXTRA_RESUME, true))
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

    override fun onPause() {
        super.onPause()
        saveVisibleNames()
    }

    /***
     * Keep what is written, the names are shared with the game
     */
    private fun saveVisibleNames() {
        if (nameFields.isEmpty()) return
        PlayerNamesStore.merge(this, nameFields.map { it.text.toString().trim() })
    }

    override fun onResume() {
        super.onResume()

        //A name changed during the game shows here
        val saved = PlayerNamesStore.readAll(this)
        nameFields.forEachIndexed { i, field ->
            val name = saved.getOrNull(i) ?: ""
            if (field.text.toString() != name) field.setText(name)
        }
        if (::extensionsBox.isInitialized) fillExtensions()
        if (::resumeButton.isInitialized) resumeButton.visibility = if (GameSaveStore.exists(this)) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────────────────────────
    //  Extensions
    // ──────────────────────────────────────────────────────────────

    private fun card(title: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBackground(this@SelectActivity)
            setPadding(dp(14), dp(12), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(12)) }
        }
        card.addView(TextView(this).apply {
            text = title
            setTextColor(colorOf(R.color.cyan))
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
            val roles = RolesRepository.roleTypes.filter { RolesRepository.extensionOf(it.first) == extension }

            //A box without any role is not shown (the roles of the moderator, while he wrote none)
            if (roles.isEmpty()) continue
            val enabled = ExtensionSettings.isEnabled(this, extension)

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
                        val picture = RoleImages.of(character)
                        if (picture != 0) {
                            ImageView(this).apply {
                                setImageResource(picture)
                                adjustViewBounds = true
                                contentDescription = character.name
                            }
                        } else {
                            TextView(this).apply {
                                text = character.name
                                setTextColor(colorOf(R.color.white))
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
                setTextColor(colorOf(R.color.cyan))
                textSize = 16f
            }
            val bar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = roundBg(colorOf(R.color.cyan_fill_selected), colorOf(R.color.cyan), dp(1), dp(10))
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
                text = Labels.extension(this@SelectActivity, extension) + "  (" + roles.size + ")"
                setTextColor(colorOf(R.color.white))
                textSize = 16f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            bar.addView(SwitchCompat(this).apply {
                isChecked = enabled
                setOnCheckedChangeListener { _, checked ->
                    ExtensionSettings.setEnabled(this@SelectActivity, extension, checked)
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
            setTextColor(colorOf(R.color.white))
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
                PlayerNamesStore.saveAll(this@SelectActivity, emptyList())
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        //Names, one line per player
        namesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(namesBox)

        val saved = PlayerNamesStore.readAll(this)
        for (i in 0 until count) addNameRow(i, saved.getOrNull(i) ?: "")
    }

    private fun addNameRow(index: Int, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

        row.addView(TextView(this).apply {
            text = (index + 1).toString() + "."
            setTextColor(colorOf(R.color.hint))
            textSize = 15f
        }, LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT))

        val field = EditText(this).apply {
            setText(value)
            hint = getString(R.string.SeatDefaultName, index + 1)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(colorOf(R.color.white))
            setHintTextColor(colorOf(R.color.placeholder))
        }
        row.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        nameFields.add(field)
        namesBox.addView(row)
    }

    private fun changeCount(newCount: Int) {
        val wanted = newCount.coerceIn(minPlayers, maxPlayers)
        if (wanted == count) return

        //The names already written stay
        val saved = PlayerNamesStore.readAll(this)
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
        if (!ExtensionSettings.hasEnabled(this)) {
            Toast.makeText(this, R.string.Select_NoExtension, Toast.LENGTH_SHORT).show()
            return
        }

        //The names are kept for the next games (the names of the players that don't exist now are kept too)
        val names = nameFields.map { it.text.toString().trim() }
        PlayerNamesStore.merge(this, names)
        PlayerNamesStore.saveCount(this, count)

        //A new game replaces the one that was left
        GameSaveStore.clear(this)

        val intent = Intent(this, GameActivity::class.java)
        intent.putStringArrayListExtra(GameActivity.EXTRA_PLAYER_NAMES, ArrayList(names))
        startActivity(intent)
    }
}
