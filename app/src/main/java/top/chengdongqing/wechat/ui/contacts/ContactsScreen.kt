package top.chengdongqing.wechat.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Contact
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun ContactsScreen() {
    val contactsGrouped = remember {
        generateRandomContacts(100)
    }
    val initials = contactsGrouped.keys.toList().sorted()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.background)
    ) {
        LazyColumn {
            // 顶部固定功能项
            item { TopFunctionList() }

            // 联系人分组列表
            contactsGrouped.forEach { (initial, contacts) ->
                stickyHeader {
                    ContactHeader(initial)
                }

                itemsIndexed(contacts) { index, contact ->
                    Column(modifier = Modifier.background(Color.White)) {
                        ContactItem(contact)

                        if (index < contacts.size - 1) {
                            WeDivider(modifier = Modifier.padding(start = 68.dp))
                        }
                    }
                }
            }
        }

        // 右侧字母索引栏
        AlphabetIndexer(
            modifier = Modifier.align(Alignment.CenterEnd),
            initials = initials
        )
    }
}

fun generateRandomContacts(count: Int): Map<String, List<Contact>> {
    val surnames = listOf(
        "阿",
        "巴",
        "陈",
        "戴",
        "鄂",
        "付",
        "高",
        "何",
        "金",
        "李",
        "马",
        "牛",
        "彭",
        "秦",
        "苏",
        "万",
        "夏",
        "张"
    )
    val names = listOf("强", "玲", "伟", "芳", "杰", "秀", "涛", "娜", "军", "明")

    return List(count) { i ->
        val surname = surnames.random()
        val name = names.random()
        val fullName = "$surname$name"

        // 简单模拟获取首字母 (实际开发建议用 Pinyin4j 等库获取拼音首字母)
        // 这里为了演示，直接取姓氏作为首字母分组标识
        Contact(
            name = if (i == 0) "文件传输助手" else fullName,
            avatar = R.drawable.img_logo,
            initial = if (i == 0) "星" else surname // 微信中特殊联系人常排在前面或用星号
        )
    }
        .groupBy { it.initial } // 根据首字母分组
        .toSortedMap()          // 按照 A-Z 排序
}