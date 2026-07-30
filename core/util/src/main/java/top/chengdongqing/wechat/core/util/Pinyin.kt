package top.chengdongqing.wechat.core.util

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum
import com.github.houbb.pinyin.util.PinyinHelper

/**
 * 获取首字母
 */
fun String.getInitial(): Char {
    if (this.isBlank()) return '#'

    val firstChar = this.first()

    return if (firstChar.isChinese) {
        PinyinHelper.toPinyin(this, PinyinStyleEnum.FIRST_LETTER).first()
    } else {
        firstChar
    }.uppercaseChar().let {
        if (it in 'A'..'Z') {
            it
        } else {
            '#'
        }
    }
}

/**
 * 是否是中文字符
 */
val Char.isChinese: Boolean
    get() = this.code in 0x4E00..0x9FFF
