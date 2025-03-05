package Model

import main.werewolfkotlin.R

class ImageGetter {

    companion object {
        fun GetImage(classObject : Character) : Int {
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
    }
}
