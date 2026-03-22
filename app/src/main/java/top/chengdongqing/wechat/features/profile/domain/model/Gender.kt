package top.chengdongqing.wechat.features.profile.domain.model

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
        val Gender?.safePronoun: Int
            get() = this?.pronoun ?: Male.pronoun
    }
}