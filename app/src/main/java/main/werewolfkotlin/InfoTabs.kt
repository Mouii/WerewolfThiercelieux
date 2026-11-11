package main.werewolfkotlin

import android.graphics.Color
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

/***
 * Content of the tabs "Players", "Roles" and "Historic" of the information window
 */
class InfoTabs(private val activity: AppCompatActivity) {

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()

    private fun card(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#444444"))
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
            val holders = seats.filter { s -> s.character != null && members.any { it === s.character } }

            val block = card()
            block.addView(text(
                if (members.size > 1) "${first.name} ×${members.size}" else first.name,
                18f, Color.WHITE, true
            ))

            for (seat in holders) {
                block.addView(text(playerName(seat), 16f, Color.parseColor("#00E5FF")))
            }

            val missing = members.size - holders.size
            if (missing > 0) {
                block.addView(text(activity.getString(R.string.InformationView_NotAssigned, missing), 14f, Color.parseColor("#999999")))
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
        if (block.kind == EffectBlock.Kind.INFECTION) seats.filter { it.character?.infected == true }
        else block.players

    private fun buildEffectBlock(block: EffectBlock, container: LinearLayout): View {
        val view = card()

        //Title, with the option of the infect father or a button to delete the block
        val header = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(text(block.title, 18f, Color.WHITE, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        if (block.kind == EffectBlock.Kind.INFECTION) {
            val soloSwitch = SwitchCompat(activity).apply {
                text = activity.getString(R.string.InformationView_InfectSolo)
                setTextColor(Color.WHITE)
                textSize = 12f
                isChecked = GameState.fatherCanInfectSolo
                setOnCheckedChangeListener { _, checked -> GameState.fatherCanInfectSolo = checked }
            }
            header.addView(soloSwitch)
        } else {
            val delete = text("✕", 18f, Color.parseColor("#FF6B6B"), true).apply {
                setPadding(dp(12), dp(4), dp(4), dp(4))
                setOnClickListener {
                    GameState.effectBlocks.remove(block)
                    fillRoles(container)
                }
            }
            header.addView(delete)
        }
        view.addView(header)

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
                        GameState.disinfect(activity, seat)
                    } else {
                        block.players.remove(seat)
                        GameState.log(activity, R.string.History_TargetRemoved, block.title, seat.name)
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
        view.addView(buttons)

        return view
    }

    /***
     * The moderator picks a player to add in the block
     */
    private fun chooseAndAdd(block: EffectBlock, container: LinearLayout) {
        val candidates: List<Seat> = if (block.kind == EffectBlock.Kind.INFECTION) {
            //All the players who are not werewolves yet. Solo roles only if the option allows it
            seats.filter { s ->
                val c = s.character
                s.alive && c != null && !c.isWerewolf && !c.infected && (GameState.fatherCanInfectSolo || !c.isSolo)
            }
        } else {
            seats.filter { s -> block.players.none { it === s } }
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
                    GameState.infect(activity, seat)
                } else {
                    block.players.add(seat)
                    GameState.log(activity, R.string.History_Target, block.title, seat.name)
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
            container.addView(text(activity.getString(R.string.InformationView_HistoryEmpty), 15f, Color.parseColor("#999999")).apply {
                setPadding(dp(12), dp(12), dp(12), dp(12))
            })
            return
        }

        for (entry in GameState.history.asReversed()) {
            val phaseName = WorkerEasier.mapPhaseTranslation[entry.phase] ?: entry.phase.name
            val line = card()
            line.addView(text(activity.getString(R.string.History_Stamp, entry.turn, phaseName), 12f, Color.parseColor("#00E5FF")))
            line.addView(text(entry.text, 15f, Color.WHITE))
            container.addView(line)
        }
        container.addView(View(activity), LinearLayout.LayoutParams(1, dp(8)))
    }
}
