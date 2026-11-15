package main.werewolfkotlin.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import main.werewolfkotlin.R
import main.werewolfkotlin.data.Information
import main.werewolfkotlin.game.GameState
import main.werewolfkotlin.ui.design.DoubleClickListener
import main.werewolfkotlin.ui.dialog.PanelDialog

/***
 * The free blocks of the "Infos" tab : a name, a text, the players to add, drag'n'drop to sort them
 */
class InformationAdapter(
    private val informations: MutableList<Information>,
    private val onSave: () -> Unit //Save function
) : RecyclerView.Adapter<InformationAdapter.ViewHolder>() {

    //Reference to touchHelper after activity creation
    var touchHelper: ItemTouchHelper? = null

    // ── ViewHolder : how the card is represented
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameLabel: TextView = view.findViewById(R.id.nameLabel)
        val descriptionField: EditText  = view.findViewById(R.id.descriptionField)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)
        val holdForDrag: View = view.findViewById(R.id.dragHold)
        val addPlayerButton: View = view.findViewById(R.id.btnAddPlayer)
        val clearButton: View = view.findViewById(R.id.btnClearText)

        // Create only one time the listeners
        init {

            // Get activity
            val activity = itemView.context as AppCompatActivity

            nameLabel.setOnClickListener(object : DoubleClickListener() {
                override fun onDoubleClick() {
                    val pos = bindingAdapterPosition
                    if (pos < 0) return
                    val input = EditText(activity).apply { setText(informations[pos].name) }
                    AlertDialog.Builder(activity)   // ← activity au lieu de itemView.context
                        .setTitle(R.string.InformationView_ChangeName)
                        .setView(input)
                        .setPositiveButton(R.string.Generic_Okay) { _, _ ->
                            informations[pos].name = input.text.toString()
                            nameLabel.text = informations[pos].name
                            onSave()
                        }.show()
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vue = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_information, parent, false)
        return ViewHolder(vue)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val information = informations[position]
        val estLePremier = false //No block is mandatory anymore : the deaths moved to the roles

        // ── Display
        holder.nameLabel.text = information.name
        holder.descriptionField.setText(information.description)

        // Dynamic update. Remove watcher, put text so that the update is effective
        val ancienWatcher = holder.descriptionField.tag as? android.text.TextWatcher
        holder.descriptionField.removeTextChangedListener(ancienWatcher)
        holder.descriptionField.setText(information.description)

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(e: android.text.Editable?) {
                // Getting position in case of drag'n'drop
                val pos = holder.bindingAdapterPosition
                if (pos >= 0) {
                    informations[pos].description = e?.toString() ?: ""
                    onSave()
                }
            }
        }
        holder.descriptionField.tag = watcher
        holder.descriptionField.addTextChangedListener(watcher)

        // Add the name of a player in the text, in plain text. The watcher above saves it
        holder.addPlayerButton.setOnClickListener {
            val activity = holder.itemView.context as AppCompatActivity
            if (GameState.seats.isEmpty()) return@setOnClickListener

            val panel = PanelDialog(activity, activity.getString(R.string.InformationView_AddPlayer))
            for (seat in GameState.seats) {
                panel.body.addView(panel.choiceRow(0, if (seat.alive) seat.name else seat.name + " ✝", false) {
                    val current = holder.descriptionField.text.toString()
                    holder.descriptionField.setText(if (current.isBlank()) seat.name else current.trimEnd() + ", " + seat.name)
                    panel.dismiss()
                })
            }
            panel.addButton(activity.getString(R.string.Generic_Cancel))
            panel.show()
        }

        // Empty the text in one tap
        holder.clearButton.setOnClickListener {
            holder.descriptionField.setText("")
        }

        // Allow deletion except the first
        if (estLePremier) {
            holder.deleteButton.visibility = View.INVISIBLE
            holder.holdForDrag.visibility  = View.INVISIBLE
        } else {
            holder.deleteButton.visibility = View.VISIBLE
            holder.holdForDrag.visibility  = View.VISIBLE

            holder.deleteButton.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                informations.removeAt(pos)
                notifyItemRemoved(pos)
                onSave()
            }

            // Drag & drop
            holder.holdForDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(holder)
                }
                false
            }
        }
    }

    override fun getItemCount() = informations.size

    // Callback drag & drop
    // Allow move of drag'n'drop except on the first one
    val dragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val startPosition  = vh.bindingAdapterPosition
            val endPosition = target.bindingAdapterPosition
            val item = informations.removeAt(startPosition)
            informations.add(endPosition, item)
            notifyItemMoved(startPosition, endPosition)
            onSave()
            return true
        }
        override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
    }
}
