package Model

import main.werewolfkotlin.EnumPhase
import main.werewolfkotlin.R

class ImageGetter {

    companion object {
        fun getCharacterImage(classObject : Character) : Int {
            when(classObject) {
                is Cupid -> {
                    return R.drawable.cupid
                }
                is Hunter -> {
                    return R.drawable.hunter
                }
                is LittleGirl -> {
                    return R.drawable.littlegirl
                }
                is Seer -> {
                    return R.drawable.seer
                }
                is Thief -> {
                    return R.drawable.thief
                }
                is Villager -> {
                    return R.drawable.villager
                }
                is Werewolf -> {
                    return R.drawable.werewolf
                }
                is Witch -> {
                    return R.drawable.witch
                }
            }

            return 0
        }

        fun getBackgroundImage(phase: EnumPhase) : Int {
            return when(phase) {
                EnumPhase.SLEEPING -> R.drawable.village_night
                EnumPhase.NIGHT -> R.drawable.village_night
                EnumPhase.DAWN -> R.drawable.village_dawn
                EnumPhase.DAY -> R.drawable.village_day
                EnumPhase.SUNSET -> R.drawable.village_sunset
            }

        }
    }
}
