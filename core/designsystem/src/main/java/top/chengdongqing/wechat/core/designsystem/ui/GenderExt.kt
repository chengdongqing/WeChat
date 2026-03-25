package top.chengdongqing.wechat.core.designsystem.ui

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.Gender

@get:StringRes
val Gender.labelRes: Int
    get() = when (this) {
        Gender.Male -> R.string.gender_male
        Gender.Female -> R.string.gender_female
    }

@get:StringRes
val Gender.pronounRes: Int
    get() = when (this) {
        Gender.Male -> R.string.gender_male_pronoun
        Gender.Female -> R.string.gender_female_pronoun
    }

val Gender?.safePronounRes: Int
    get() = (this ?: Gender.Male).pronounRes