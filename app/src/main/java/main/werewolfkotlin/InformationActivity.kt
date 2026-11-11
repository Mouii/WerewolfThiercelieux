package main.werewolfkotlin

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import main.werewolfkotlin.databinding.ActivityInformationBinding

data class Information(
    var name: String,
    var description: String
)

class InformationActivity : AppCompatActivity() {

    //Object from the xml view to get all the elements
    private lateinit var binding: ActivityInformationBinding

    //List of information blocks
    private val informations = mutableListOf<Information>()

    /***
     * Execution on creation of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Load blocks before adapter
        load()

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter  = InformationAdapter(informations)  { save() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Drag & drop : link ItemTouchHelper to Adapter
        val touchHelper = ItemTouchHelper(adapter.dragCallback)
        touchHelper.attachToRecyclerView(recycler)
        adapter.touchHelper = touchHelper //Needed to start drag

        // Listener on adding button
        findViewById<Button>(R.id.btnAjouter).setOnClickListener {
            informations.add(Information(getString(R.string.InformationView_NewName), ""))
            adapter.notifyItemInserted(informations.size - 1)
        }

        //Tabs : players / effects of the roles / free information / historic
        val infoTabs = InfoTabs(this)
        val tabs = binding.tabLayout
        listOf(
            R.string.InformationView_TabPlayers,
            R.string.InformationView_TabRoles,
            R.string.InformationView_TabInfos,
            R.string.InformationView_TabHistory
        ).forEach { tabs.addTab(tabs.newTab().setText(it)) }

        fun showTab(position: Int) {
            binding.playersScroll.visibility = if (position == 0) View.VISIBLE else View.GONE
            binding.rolesPanel.visibility = if (position == 1) View.VISIBLE else View.GONE
            binding.infoPanel.visibility = if (position == 2) View.VISIBLE else View.GONE
            binding.historyScroll.visibility = if (position == 3) View.VISIBLE else View.GONE

            //The content is always rebuilt, so it follows the game
            when (position) {
                0 -> infoTabs.fillPlayers(binding.playersContainer)
                1 -> infoTabs.fillRoles(binding.rolesContainer)
                3 -> infoTabs.fillHistory(binding.historyContainer)
            }
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnAddBlock.setOnClickListener { infoTabs.askNewBlock(binding.rolesContainer) }
        showTab(0)

        //Listener on button
        binding.returnButton.setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        val prefs = getSharedPreferences("informations", Context.MODE_PRIVATE)
        val json  = org.json.JSONArray()
        for (p in informations) {
            json.put(org.json.JSONObject().apply {
                put("name", p.name)
                put("description", p.description)
            })
        }
        prefs.edit().putString("list", json.toString()).apply()
    }

    private fun load() {
        val prefs = getSharedPreferences("informations", Context.MODE_PRIVATE)
        val json  = prefs.getString("list", null)

        if (json != null) {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                informations.add(Information(
                    name = obj.getString("name"),
                    description = obj.getString("description")
                ))
            }
        } else {
            //First launch : value by default
            informations.add(Information("Morts", ""))
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sweepRight() {
        //In order to transfer the list to the other activity, we create an intent
        //in direction of the game activity
        val intent = Intent(this, GameActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        //Start the other activity
        startActivity(intent)
    }

}

// ADAPTER
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
        val estLePremier = position == 0 //First block doesn't move

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
            if (endPosition == 0) return false  // don't go on the first bloc
            val item = informations.removeAt(startPosition)
            informations.add(endPosition, item)
            notifyItemMoved(startPosition, endPosition)
            onSave()
            return true
        }
        override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
    }
}

// HELPER double-tap = utility class for double tape
abstract class DoubleClickListener : View.OnClickListener {
    private var lastClick = 0L
    override fun onClick(v: View) {
        val now = System.currentTimeMillis()
        if (now - lastClick < 300) onDoubleClick()
        lastClick = now
    }
    abstract fun onDoubleClick()
}