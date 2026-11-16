package main.werewolfkotlin.ui.info

import main.werewolfkotlin.ui.Narrator

import main.werewolfkotlin.ui.design.colorOf

import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import main.werewolfkotlin.R
import main.werewolfkotlin.game.EffectBlock
import main.werewolfkotlin.game.EffectBlocks
import main.werewolfkotlin.game.GameMessage
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import main.werewolfkotlin.ui.Labels
import main.werewolfkotlin.ui.design.dp
import model.kindOf

/***
 * Content of the tabs "Players", "Roles" and "Historic" of the information window
 */
class InfoTabs(private val activity: AppCompatActivity) {

    private fun dp(value: Int): Int = activity.dp(value)

    private fun card(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(activity.colorOf(R.color.card))
        setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(8), dp(8), dp(8), 0) }
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(activity).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun playerName(seat: Seat): String =
        if (seat.alive) seat.name else seat.name + " ✝"

    // ──────────────────────────────────────────────────────────────
    //  Players : the roles of the game and who has them (read only)
    // ──────────────────────────────────────────────────────────────
    fun fillPlayers(container: LinearLayout) {
        container.removeAllViews()

        val groups = GameState.allCharacters.sortedBy { it.order }.groupBy { kindOf(it) }.values
        for (members in groups) {
            val first = members.first()
            val holders = GameState.seats.filter { s -> s.character != null && members.any { it === s.character } }

            val block = card()
            block.addView(text(
                if (members.size > 1) "${first.name} (${members.size})" else first.name,
                18f, activity.colorOf(R.color.white), true
            ))

            for (seat in holders) {
                block.addView(text(playerName(seat), 16f, activity.colorOf(R.color.cyan)))
            }

            container.addView(block)
        }

        //Players who have no role yet
        val withoutRole = GameState.seats.filter { it.character == null }
        if (withoutRole.isNotEmpty()) {
            val block = card()
            block.addView(text(activity.getString(R.string.InformationView_NoRole), 18f, activity.colorOf(R.color.no_role), true))
            for (seat in withoutRole) {
                block.addView(text(playerName(seat), 16f, activity.colorOf(R.color.no_role)))
            }
            container.addView(block)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Roles : effects of the roles, filled by selecting players
    // ──────────────────────────────────────────────────────────────
    fun fillRoles(container: LinearLayout) {
        container.removeAllViews()
        for (block in GameState.effectBlocks.toList()) {
            container.addView(buildEffectBlock(block, container))
        }
    }

    /***
     * Players of a block. The infected block is directly made from the status of the players
     */
    private fun playersOf(block: EffectBlock): List<Seat> =
        if (block.kind == EffectBlock.Kind.INFECTION) GameState.seats.filter { it.character?.isInfected == true }
        else block.players

    private fun buildEffectBlock(block: EffectBlock, container: LinearLayout): View {
        val view = card()

        //Title, with the option of the infect father or a button to delete the block
        val header = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(text(block.title, 18f, activity.colorOf(R.color.white), true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        when (block.kind) {
            EffectBlock.Kind.INFECTION -> {
                val soloSwitch = SwitchCompat(activity).apply {
                    text = activity.getString(R.string.InformationView_InfectSolo)
                    setTextColor(activity.colorOf(R.color.white))
                    textSize = 12f
                    isChecked = GameState.fatherCanInfectSolo
                    setOnCheckedChangeListener { _, checked -> GameState.fatherCanInfectSolo = checked }
                }
                header.addView(soloSwitch)
            }
            EffectBlock.Kind.TARGETS -> {
                val delete = text("✕", 18f, activity.colorOf(R.color.danger), true).apply {
                    setPadding(dp(12), dp(4), dp(4), dp(4))
                    setOnClickListener {
                        GameState.effectBlocks.remove(block)
                        fillRoles(container)
                    }
                }
                header.addView(delete)
            }
            //Nothing to add : the deaths have their own button below, a copied player is only shown
            EffectBlock.Kind.DEATHS, EffectBlock.Kind.MODEL -> {}
        }
        view.addView(header)

        //Option of the role : red light on its targets during its turn, green light on it during their turn
        if (block.kind == EffectBlock.Kind.TARGETS && block.roleKey != null) {
            val lightSwitch = SwitchCompat(activity).apply {
                text = activity.getString(R.string.Block_Lights)
                setTextColor(activity.colorOf(R.color.hint))
                textSize = 12f
                isChecked = block.lights
                setOnCheckedChangeListener { _, checked -> block.lights = checked }
            }
            view.addView(lightSwitch)
        }

        //Players, a tap selects them
        val chips = ChipGroup(activity)
        val chipSeats = mutableMapOf<Chip, Seat>()
        for (seat in playersOf(block)) {
            val chip = Chip(activity).apply {
                text = playerName(seat)
                isCheckable = true
            }
            chipSeats[chip] = seat
            chips.addView(chip)
        }
        view.addView(chips)

        //The copied player is only shown : the copy cannot be undone
        if (block.kind == EffectBlock.Kind.MODEL) return view

        val buttons = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        val add = MaterialButton(activity).apply {
            setText(R.string.InformationView_AddPlayer)
            setOnClickListener { chooseAndAdd(block, container) }
        }
        val remove = MaterialButton(activity).apply {
            setText(R.string.InformationView_RemoveSelected)
            setOnClickListener {
                val selected = chipSeats.filter { it.key.isChecked }.values
                for (seat in selected) {
                    if (block.kind == EffectBlock.Kind.INFECTION) {
                        RoleRoster.disinfect(seat)
                    } else {
                        block.players.remove(seat)
                        if (block.kind == EffectBlock.Kind.TARGETS)
                            GameState.log(GameMessage.TARGET_REMOVED, block.title, seat.name)
                    }
                }
                fillRoles(container)
            }
        }
        buttons.addView(add)
        buttons.addView(remove, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })

        //The block of the deaths is used a lot : one button empties it
        if (block.kind == EffectBlock.Kind.DEATHS) {
            val clear = MaterialButton(activity).apply {
                setText(R.string.Block_Clear)
                setOnClickListener {
                    block.players.clear()
                    fillRoles(container)
                }
            }
            buttons.addView(clear, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) })
        }
        view.addView(buttons)

        return view
    }

    /***
     * The moderator picks a player to add in the block
     */
    private fun chooseAndAdd(block: EffectBlock, container: LinearLayout) {
        val candidates: List<Seat> = if (block.kind == EffectBlock.Kind.INFECTION) {
            //All the players who are not werewolves yet. Solo roles only if the option allows it
            GameState.seats.filter { s ->
                val c = s.character
                s.alive && c != null && !c.isWerewolf && !c.isInfected && (GameState.fatherCanInfectSolo || !c.isSolo)
            }
        } else {
            GameState.seats.filter { s -> block.players.none { it === s } }
        }

        if (candidates.isEmpty()) {
            Toast.makeText(activity, R.string.InformationView_NoPlayer, Toast.LENGTH_SHORT).show()
            return
        }

        val names = candidates.map { s ->
            val role = s.character?.name
            if (role != null) "${playerName(s)} — $role" else playerName(s)
        }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(block.title)
            .setItems(names) { _, which ->
                val seat = candidates[which]
                if (block.kind == EffectBlock.Kind.INFECTION) {
                    RoleRoster.infect(seat)
                } else {
                    EffectBlocks.addPlayer(block, seat)
                    if (block.kind == EffectBlock.Kind.TARGETS)
                        GameState.log(GameMessage.TARGET, block.title, seat.name)
                }
                fillRoles(container)
            }
            .setNegativeButton(R.string.Generic_Cancel, null)
            .show()
    }

    /***
     * Add a block with a name chosen by the moderator
     */
    fun askNewBlock(container: LinearLayout) {
        val input = EditText(activity).apply {
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val holder = LinearLayout(activity).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.InformationView_NewBlock)
            .setView(holder)
            .setPositiveButton(R.string.Generic_Okay) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    GameState.effectBlocks.add(EffectBlock(title))
                    fillRoles(container)
                }
            }
            .setNegativeButton(R.string.Generic_Cancel, null)
            .show()
    }

    // ──────────────────────────────────────────────────────────────
    //  Historic : everything that happened, the last action first
    // ──────────────────────────────────────────────────────────────
    fun fillHistory(container: LinearLayout) {
        container.removeAllViews()

        if (GameState.history.isEmpty()) {
            container.addView(text(activity.getString(R.string.InformationView_HistoryEmpty), 15f, activity.colorOf(R.color.empty_list)).apply {
                setPadding(dp(12), dp(12), dp(12), dp(12))
            })
            return
        }

        for (entry in GameState.history.asReversed()) {
            val phaseName = Labels.phase(activity, entry.phase).ifEmpty { entry.phase.name }
            val line = card()
            line.addView(text(activity.getString(R.string.History_Stamp, entry.turn, phaseName), 12f, activity.colorOf(R.color.cyan)))
            line.addView(text(Narrator.say(activity, entry.text), 15f, activity.colorOf(R.color.white)))
            container.addView(line)
        }
        container.addView(View(activity), LinearLayout.LayoutParams(1, dp(8)))
    }
}
