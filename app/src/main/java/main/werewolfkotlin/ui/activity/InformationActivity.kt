package main.werewolfkotlin.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import main.werewolfkotlin.R
import main.werewolfkotlin.data.Information
import main.werewolfkotlin.data.InformationStore
import main.werewolfkotlin.databinding.ActivityInformationBinding
import main.werewolfkotlin.ui.adapter.InformationAdapter
import main.werewolfkotlin.ui.info.InfoTabs

/***
 * Information window of the game : players, effects of the roles, free notes, historic
 */
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

        //Load blocks before adapter. The block of the deaths lives in the "Roles" tab, the free blocks start empty
        informations.addAll(InformationStore.load(this))

        val adapter = InformationAdapter(informations) { save() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Drag & drop : link ItemTouchHelper to Adapter
        val touchHelper = ItemTouchHelper(adapter.dragCallback)
        touchHelper.attachToRecyclerView(binding.recyclerView)
        adapter.touchHelper = touchHelper //Needed to start drag

        // Listener on adding button
        binding.btnAjouter.setOnClickListener {
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

    private fun save() = InformationStore.save(this, informations)
}
