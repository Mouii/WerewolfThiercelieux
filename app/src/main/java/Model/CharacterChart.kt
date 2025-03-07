package Model

import android.widget.ImageView

data class CharacterChart(val name : String,
                          var occurrence : Int,
                          val maxOccurrence : Int,
                          val image : ImageView
)
