package top.chengdongqing.wechat.core.model

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

/**
 * 性别枚举
 */
@Serializable
enum class Gender(
    @get:StringRes val labelRes: Int,
    @get:StringRes val pronounRes: Int
) {
    Male(R.string.gender_male, R.string.gender_male_pronoun),
    Female(R.string.gender_female, R.string.gender_female_pronoun);

    companion object {
        val Gender?.safePronoun: Int
            get() = this?.pronounRes ?: Male.pronounRes
    }
}
