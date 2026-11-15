package main.werewolfkotlin.ui.adapter

import main.werewolfkotlin.R

import main.werewolfkotlin.ui.design.colorOf

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/***
 * Texts of the spinners : black on white, centered
 */
private fun TextView.asSpinnerItem(): TextView {
    gravity = Gravity.CENTER
    setTextColor(colorOf(R.color.black))
    setBackgroundColor(colorOf(R.color.white))
    return this
}

/***
 * Spinner adapter simple
 */
class SpinnerAdapter(context: Context, items: List<String>) :
    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        (super.getView(position, convertView, parent) as TextView).asSpinnerItem()

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        (super.getDropDownView(position, convertView, parent) as TextView).asSpinnerItem()
}

/***
 * Spinner adapter on pair to print the correct associate language but keep the key
 */
class SpinnerPairAdapter(context: Context, private val items: List<Pair<String, String>>) :
    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items.map { it.second }) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = items[position].second // Display the associated text
        return view.asSpinnerItem()
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        view.text = items[position].second // Display the associated text
        return view.asSpinnerItem()
    }

    fun getValue(position: Int): String = items[position].first // Retrieve the associated value
}
