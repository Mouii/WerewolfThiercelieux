package main.werewolfkotlin.ui.dialog

import main.werewolfkotlin.ui.design.colorOf

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import main.werewolfkotlin.R
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.game.RoleRoster
import main.werewolfkotlin.game.Seat
import main.werewolfkotlin.game.TableActions
import main.werewolfkotlin.ui.design.RoleImages
import main.werewolfkotlin.ui.design.dp
import model.CharacterGame

/***
 * Random roles, in steps :
 * 1. the moderator chooses the roles to give (and the variant of each one)
 * 2. a question reminds what will happen
 * 3. if players who already have a role have to be replaced, the moderator chooses them
 * [onApplied] is called once the roles are given.
 */
class RandomRolesDialog(private val activity: Activity, private val onApplied: () -> Unit) {

    fun show() {
        if (GameState.seats.none { it.alive } || GameState.catalog.isEmpty()) {
            Toast.makeText(activity, R.string.RandomRoles_None, Toast.LENGTH_SHORT).show()
            return
        }
        showSelection()
    }

    /***
     * Step 1 : the roles to give. Tap a card to add one (a window asks the variant if there are some),
     * long press to remove one. At the limit of the role, a tap removes them all.
     * A role that exists in several copies (sisters, brothers, werewolves...) asks at its first tap
     * whether one copy or all of them are wanted.
     */
    private fun showSelection() {
        val panel = PanelDialog(activity, activity.getString(R.string.RandomRoles_Title))
        val chosen = mutableListOf<CharacterGame>()
        val groups = GameState.catalog.groupBy { it.className }.values.sortedBy { it.first().name }

        panel.body.addView(panel.text(activity.getString(R.string.RandomPick_Help), 12f, activity.colorOf(R.color.hint)))
        val gridHolder = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        panel.body.addView(gridHolder)

        //Copies of a role that can still be added (the roles already given count)
        fun remaining(variants: List<CharacterGame>): Int {
            val className = variants.first().className
            return variants.first().maxOccurrence - RoleRoster.countOfClass(className) - chosen.count { it.className == className }
        }

        panel.addButton(activity.getString(R.string.Generic_Cancel))
        val validate = panel.addButton(activity.getString(R.string.Generic_Okay), accent = true, closes = false) {
            showConfirm(chosen.toList(), panel)
        }

        fun render() {
            gridHolder.removeAllViews()

            val tiles = groups.map { variants ->
                val first = variants.first()
                val count = chosen.count { it.className == first.className }

                val tile = panel.cardTile(RoleImages.of(first), first.name, count > 0) {
                    if (remaining(variants) > 0) {
                        //Once the variant is known : one copy, or the choice between one and all of them
                        val add: (CharacterGame) -> Unit = { variant ->
                            val left = remaining(variants)
                            if (count == 0 && left > 1) {
                                askHowMany(variant, left) { copies ->
                                    repeat(copies) { chosen.add(variant) }
                                    render()
                                }
                            } else {
                                chosen.add(variant)
                                render()
                            }
                        }

                        if (variants.size > 1) VariantDialog.show(activity, variants, 0, add) else add(first)
                    } else if (count > 0) {
                        chosen.removeAll { it.className == first.className }
                        render()
                    }
                }
                tile.setOnLongClickListener {
                    val last = chosen.lastOrNull { it.className == first.className }
                    if (last != null) {
                        chosen.remove(last)
                        render()
                    }
                    true
                }
                if (count == 0 && remaining(variants) <= 0) tile.alpha = 0.35f

                //Number of copies chosen, on the card
                val frame = FrameLayout(activity)
                frame.addView(tile, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
                if (count > 0) {
                    frame.addView(TextView(activity).apply {
                        text = "×$count"
                        textSize = 13f
                        setTextColor(activity.colorOf(R.color.black))
                        setTypeface(typeface, Typeface.BOLD)
                        setBackgroundColor(activity.colorOf(R.color.cyan))
                        setPadding(activity.dp(5) , 0, activity.dp(5), 0)
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.END))
                }
                frame
            }
            gridHolder.addView(panel.cardGrid(tiles))

            //Hint between the roles and the buttons
            val selected = chosen.size
            val players = GameState.seats.count { it.alive }
            val withoutRole = TableActions.roleless().size
            when {
                selected > players ->
                    panel.setHint(activity.getString(R.string.RandomPick_TooMany, players), activity.colorOf(R.color.danger))
                selected > withoutRole ->
                    panel.setHint(activity.getString(R.string.RandomPick_Replace, selected - withoutRole), activity.colorOf(R.color.warning))
                else ->
                    panel.setHint(activity.getString(R.string.RandomPick_Selected, selected, withoutRole))
            }

            val valid = selected in 1..players
            validate.isEnabled = valid
            validate.alpha = if (valid) 1f else 0.4f
        }

        render()
        panel.show()
    }

    /***
     * A role that exists in several copies : only one, or all the copies that can still be given ?
     */
    private fun askHowMany(variant: CharacterGame, left: Int, onChosen: (copies: Int) -> Unit) {
        val panel = PanelDialog(activity, variant.name)
        panel.body.addView(panel.text(activity.getString(R.string.RandomPick_HowMany)))

        panel.addButton(activity.getString(R.string.RandomPick_One)) { onChosen(1) }
        panel.addButton(activity.getString(R.string.RandomPick_All, left), accent = true) { onChosen(left) }
        panel.show()
    }

    /***
     * Step 2 : the question. It reminds if there are less roles than players without role,
     * or if players who already have a role will have to be replaced.
     */
    private fun showConfirm(chosen: List<CharacterGame>, selection: PanelDialog) {
        val selected = chosen.size
        val withoutRole = TableActions.roleless().size

        val panel = PanelDialog(activity, activity.getString(R.string.RandomRoles_Title))
        panel.body.addView(panel.text(activity.getString(R.string.RandomConfirm_Text, selected)))
        if (selected < withoutRole) {
            panel.body.addView(panel.text(
                activity.getString(R.string.RandomConfirm_Fewer, selected, withoutRole, withoutRole - selected),
                14f, activity.colorOf(R.color.warning)))
        }
        if (selected > withoutRole) {
            panel.body.addView(panel.text(
                activity.getString(R.string.RandomConfirm_Replace, selected - withoutRole),
                14f, activity.colorOf(R.color.warning)))
        }

        panel.addButton(activity.getString(R.string.Generic_Cancel))
        panel.addButton(activity.getString(R.string.Generic_Yes), accent = true) {
            if (selected > withoutRole) {
                showReplacePicker(chosen, selected - withoutRole, selection)
            } else {
                giveRoles(chosen, emptyList())
                selection.dismiss()
            }
        }
        panel.show()
    }

    /***
     * Step 3 : the players who already have a role, the moderator checks the ones that are randomized.
     * As many as needed, so every chosen role finds a player.
     */
    private fun showReplacePicker(chosen: List<CharacterGame>, needed: Int, selection: PanelDialog) {
        val candidates = GameState.seats.filter { it.alive && it.character != null }

        val panel = PanelDialog(activity, activity.getString(R.string.RandomReplace_Title))
        val help = panel.text("", 13f, activity.colorOf(R.color.warning))
        panel.body.addView(help)

        val boxes = mutableListOf<Pair<CheckBox, Seat>>()
        lateinit var validate: TextView

        fun update() {
            val checked = boxes.count { it.first.isChecked }
            help.text = activity.getString(R.string.RandomReplace_Help, needed, checked)
            val valid = checked == needed
            validate.isEnabled = valid
            validate.alpha = if (valid) 1f else 0.4f
        }

        for (seat in candidates) {
            val character = seat.character ?: continue
            val box = CheckBox(activity).apply {
                buttonTintList = ColorStateList.valueOf(activity.colorOf(R.color.cyan))
                setOnCheckedChangeListener { _, _ -> update() }
            }
            boxes.add(Pair(box, seat))

            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.dp(6), activity.dp(4), activity.dp(6), activity.dp(4))
                isClickable = true
                setOnClickListener { box.isChecked = !box.isChecked }
            }
            row.addView(box)
            val picture = RoleImages.of(character)
            if (picture != 0) {
                row.addView(ImageView(activity).apply { setImageResource(picture) },
                    LinearLayout.LayoutParams(activity.dp(32), activity.dp(32)))
            }
            row.addView(TextView(activity).apply {
                text = seat.name + " — " + character.name
                textSize = 15f
                setTextColor(activity.colorOf(R.color.white))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = activity.dp(8) })
            panel.body.addView(row)
        }

        panel.addButton(activity.getString(R.string.Generic_Cancel))
        validate = panel.addButton(activity.getString(R.string.Generic_Okay), accent = true) {
            giveRoles(chosen, boxes.filter { it.first.isChecked }.map { it.second })
            selection.dismiss()
        }
        update()
        panel.show()
    }

    private fun giveRoles(chosen: List<CharacterGame>, replaced: List<Seat>) {
        TableActions.giveRolesAtRandom(chosen, replaced)
        onApplied()
    }
}
