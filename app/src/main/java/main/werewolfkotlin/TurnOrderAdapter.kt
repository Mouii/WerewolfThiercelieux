package main.werewolfkotlin

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/***
 * List of the turn order shown in the "Turn order" window.
 * A role can be moved by dragging its handle or with the arrows, and woken up / put to sleep with its switch.
 */
class TurnOrderAdapter(
    private var rows: MutableList<BandEntry>,
    private val onSwap: (Int, Int) -> Boolean,   //Swap two orders, false if not possible
    private val onToggle: (Int) -> Unit,         //Wake up / put to sleep the role at this order
    private val reload: () -> List<BandEntry>    //Fresh entries
) : RecyclerView.Adapter<TurnOrderAdapter.ViewHolder>() {

    //Needed to start a drag
    var touchHelper: ItemTouchHelper? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val handle: TextView = view.findViewById(R.id.dragHandle)
        val icon: ImageView = view.findViewById(R.id.orderIcon)
        val label: TextView = view.findViewById(R.id.orderLabel)
        val pending: TextView = view.findViewById(R.id.orderPending)
        val up: TextView = view.findViewById(R.id.orderUp)
        val down: TextView = view.findViewById(R.id.orderDown)
        val awake: SwitchCompat = view.findViewById(R.id.orderAwake)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_turn_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]

        holder.label.text = row.label
        if (row.iconRes != 0) holder.icon.setImageResource(row.iconRes) else holder.icon.setImageDrawable(null)
        holder.pending.visibility = if (row.pendingAwake != null) View.VISIBLE else View.GONE

        //Set the state before the listener, so that the binding doesn't trigger it
        holder.awake.setOnCheckedChangeListener(null)
        holder.awake.isChecked = row.pendingAwake ?: row.awake
        holder.awake.setOnCheckedChangeListener { _, _ ->
            val pos = holder.bindingAdapterPosition
            if (pos >= 0) {
                onToggle(rows[pos].order)
                rows = reload().toMutableList()
                notifyDataSetChanged()
            }
        }

        holder.up.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos > 0) swap(pos, pos - 1)
        }
        holder.down.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos >= 0 && pos < rows.size - 1) swap(pos, pos + 1)
        }

        holder.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper?.startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = rows.size

    /***
     * Move a row to another position, one neighbour at a time
     */
    fun swap(from: Int, to: Int): Boolean {
        if (from !in rows.indices || to !in rows.indices) return false

        val step = if (to > from) 1 else -1
        var current = from
        while (current != to) {
            val next = current + step
            val a = rows[current]
            val b = rows[next]

            //The activity changes the real order, the rows follow
            if (!onSwap(a.order, b.order)) return false
            rows[current] = b.copy(order = a.order)
            rows[next] = a.copy(order = b.order)
            notifyItemMoved(current, next)
            current = next
        }
        return true
    }

    val dragCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return swap(vh.bindingAdapterPosition, target.bindingAdapterPosition)
        }

        override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
    }
}
