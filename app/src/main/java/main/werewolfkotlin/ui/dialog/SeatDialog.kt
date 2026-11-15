package main.werewolfkotlin.ui.dialog

import android.app.Activity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import main.werewolfkotlin.R
import main.werewolfkotlin.data.PlayerNamesStore
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.NightFlow
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.colorOf
import main.werewolfkotlin.ui.design.dp
import model.CharacterGame
import model.kindOf

/***
 * Name, role, awake state and death of a player.
 * Choosing a role gives it to the player and closes the window.
 * Whatever the way the window closes, the name, the role and the states are kept.
 */
class SeatDialog(private val activity: Activity, private val events: Events) {

    /** What the game screen does after the changes */
    class Events(
        val onChanged: () -> Unit,    //Redraw the screen
        val onGameOver: () -> Unit,   //The last player died
        val onReopened: () -> Unit    //A dead player came back to a game that was over
    )

    /***
     * What the moderator chose in the window. Nothing is applied before the window closes,
     * so he can change his mind, and closing the window by the side applies the same thing.
     */
    private class Choice(val seat: Seat, val index: Int) {
        //Role of the player when the window opened
        val originalCharacter: CharacterGame? = seat.character

        //A role was chosen (null role = the role is taken away)
        var roleChanged: Boolean = false
        var chosenPrototype: CharacterGame? = null
    }

    fun show(index: Int) {
        val choice = Choice(GameState.seats[index], index)
        val panel = PanelDialog(activity, activity.getString(R.string.SeatDialog_Title))

        val nameInput = panel.field(choice.seat.name, activity.getString(R.string.SeatDialog_Name))
        panel.body.addView(nameInput)

        addRolePart(panel, choice)
        val awakeSwitch = addAwakeSwitch(panel, choice)
        val deadSwitch = addDeadSwitch(panel, choice.seat)

        panel.onDismiss = {
            applyChoice(choice, nameInput.text.toString(), awakeSwitch, deadSwitch)
            events.onChanged()
        }

        panel.addButton(activity.getString(R.string.SeatDialog_Close), accent = true)
        panel.show()
    }

    // ── The parts of the window ────────────────────────────────────

    /***
     * The role of the player : the cards of the roles he can take, in a closed bar.
     * A dead player only gets the name of his role : it cannot be changed anymore.
     */
    private fun addRolePart(panel: PanelDialog, choice: Choice) {
        val original = choice.originalCharacter

        if (!choice.seat.alive) {
            if (original != null) {
                panel.body.addView(panel.section(activity.getString(R.string.SeatDialog_RoleTitle)))
                panel.body.addView(panel.text(original.name, 16f, activity.colorOf(R.color.white), true))
            }
            return
        }

        panel.body.addView(panel.section(activity.getString(R.string.SeatDialog_RoleTitle)))

        //The roles are cards side by side (4 per line), hidden in a closed bar that shows the role of the player
        val tiles = mutableListOf<View>()

        tiles.add(panel.cardTile(0, activity.getString(R.string.SeatDialog_NoRole), original == null) {
            choice.roleChanged = original != null
            choice.chosenPrototype = null
            panel.dismiss()
        })

        for (variants in rolesToOffer(original)) {
            tiles.add(roleTile(panel, choice, variants))
        }

        //Should not happen : the role of the player is not available anymore
        if (original != null && rolesToOffer(original).none { it.first().className == original.className }) {
            tiles.add(panel.cardTile(RoleImages.of(original), original.name, true) { panel.dismiss() })
        }

        panel.body.addView(
            panel.collapsible(
                if (original != null) RoleImages.of(original) else 0,
                original?.name ?: activity.getString(R.string.SeatDialog_NoRole),
                panel.cardGrid(tiles)
            )
        )
    }

    /***
     * Roles of the activated extensions that can still be given, grouped by role.
     * The role of the player stays on top, the others follow by name.
     */
    private fun rolesToOffer(original: CharacterGame?): List<List<CharacterGame>> {
        val available = GameState.catalog.filter { prototype ->
            val inGame = RoleRoster.countOfClass(prototype.className) -
                    (if (original?.className == prototype.className) 1 else 0)
            inGame < prototype.maxOccurrence
        }

        return available.groupBy { it.className }.values
            .sortedWith(compareBy({ it.first().className != original?.className }, { it.first().name }))
    }

    /***
     * One card : the role is given right away, or a window opens when the role has variants
     */
    private fun roleTile(panel: PanelDialog, choice: Choice, variants: List<CharacterGame>): View {
        val original = choice.originalCharacter
        val first = variants.first()
        val isCurrent = original != null && first.className == original.className

        return panel.cardTile(RoleImages.of(first), first.name, isCurrent) {
            if (variants.size == 1) {
                if (!isCurrent) {
                    choice.roleChanged = true
                    choice.chosenPrototype = first
                }
                panel.dismiss()
                return@cardTile
            }

            val start = variants
                .indexOfFirst { original != null && kindOf(it) == kindOf(original) }
                .coerceAtLeast(0)

            VariantDialog.show(activity, variants, start) { chosen ->
                if (original == null || kindOf(chosen) != kindOf(original)) {
                    choice.roleChanged = true
                    choice.chosenPrototype = chosen
                }
                panel.dismiss()
            }
        }
    }

    /***
     * Wake up / put to sleep the role of this player. Only for a living player who holds a role
     */
    private fun addAwakeSwitch(panel: PanelDialog, choice: Choice): SwitchCompat? {
        val original = choice.originalCharacter
        if (!choice.seat.alive || original == null) return null

        val switch = SwitchCompat(activity).apply {
            text = activity.getString(R.string.Band_Awake)
            setTextColor(activity.colorOf(R.color.white))
            isChecked = GameState.shownAwake(original)
        }
        panel.body.addView(switch, switchParams(14))
        return switch
    }

    /***
     * Dead or alive : applied when the window closes
     */
    private fun addDeadSwitch(panel: PanelDialog, seat: Seat): SwitchCompat {
        val switch = SwitchCompat(activity).apply {
            text = activity.getString(R.string.SeatDialog_Dead)
            setTextColor(activity.colorOf(R.color.white))
            isChecked = !seat.alive
        }
        panel.body.addView(switch, switchParams(10))
        return switch
    }

    // ── Applying what was chosen ──────────────────────────────────

    /***
     * The window is closing : the name, the role, the awake state and the death are applied
     */
    private fun applyChoice(
        choice: Choice,
        typedName: String,
        awakeSwitch: SwitchCompat?,
        deadSwitch: SwitchCompat
    ) {
        val seat = choice.seat

        seat.name = typedName.trim().ifEmpty {
            activity.getString(R.string.SeatDefaultName, choice.index + 1)
        }
        PlayerNamesStore.save(activity, GameState.seats)

        if (seat.alive && choice.roleChanged) RoleRoster.setRole(seat, choice.chosenPrototype)

        applyAwake(seat, choice.originalCharacter, awakeSwitch)
        applyLife(seat, deadSwitch.isChecked)
    }

    /***
     * The awake state only changes when the player kept the same role
     */
    private fun applyAwake(seat: Seat, original: CharacterGame?, awakeSwitch: SwitchCompat?) {
        if (awakeSwitch == null) return

        val character = seat.character ?: return
        if (character !== original) return
        if (awakeSwitch.isChecked == GameState.shownAwake(character)) return

        NightFlow.setAwakeGroup(listOf(character), awakeSwitch.isChecked, character.name)
    }

    /***
     * The player is dead, or alive again
     */
    private fun applyLife(seat: Seat, dead: Boolean) {
        if (dead && seat.alive) {
            if (RoleRoster.kill(seat)) events.onGameOver()
        } else if (!dead && !seat.alive) {
            if (RoleRoster.resurrect(seat)) events.onReopened()
        }
    }

    private fun switchParams(topMargin: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { this.topMargin = activity.dp(topMargin) }
}
