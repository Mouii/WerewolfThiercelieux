package Model

import android.widget.ImageView

data class CharacterChart(val name : String,
                          var occurence : Int,
                          val maxOccurence : Int,
                          val image : ImageView
)
