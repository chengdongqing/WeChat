package top.chengdongqing.wechat.features.me.domain.model

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.R

/**
 * 性别枚举
 */
@Serializable
enum class Gender(
    @get:StringRes val label: Int,
    @get:StringRes val pronoun: Int
) {
    Male(R.string.gender_male, R.string.gender_male_pronoun),
    Female(R.string.gender_female, R.string.gender_female_pronoun);

    companion object {
        fun Gender?.getIndex(): Int = this?.ordinal ?: -1

        fun fromIndex(index: Int): Gender? = entries.getOrNull(index)

        val Gender?.safePronoun: Int
            get() = this?.pronoun ?: R.string.gender_male_pronoun
    }
}