package top.chengdongqing.wechat.core.designsystem.model

data class Emoji(
    val description: String,
    val localPath: String,
    val remoteUrl: String
)

object Emojis {
    val all = listOf(
        Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_1@2x.webp",
            description = "微笑",
            localPath = "emojis/emoji_1.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_2@2x.webp",
            description = "撇嘴",
            localPath = "emojis/emoji_2.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_3@2x.webp",
            description = "色",
            localPath = "emojis/emoji_3.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_4@2x.webp",
            description = "发呆",
            localPath = "emojis/emoji_4.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_5@2x.webp",
            description = "得意",
            localPath = "emojis/emoji_5.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_6@2x.webp",
            description = "流泪",
            localPath = "emojis/emoji_6.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_7@2x.webp",
            description = "害羞",
            localPath = "emojis/emoji_7.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_8@2x.webp",
            description = "闭嘴",
            localPath = "emojis/emoji_8.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_9@2x.webp",
            description = "睡",
            localPath = "emojis/emoji_9.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_10@2x.webp",
            description = "大哭",
            localPath = "emojis/emoji_10.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_11@2x.webp",
            description = "尴尬",
            localPath = "emojis/emoji_11.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_12@2x.webp",
            description = "发怒",
            localPath = "emojis/emoji_12.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_13@2x.webp",
            description = "调皮",
            localPath = "emojis/emoji_13.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_14@2x.webp",
            description = "呲牙",
            localPath = "emojis/emoji_14.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_15@2x.webp",
            description = "惊讶",
            localPath = "emojis/emoji_15.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_16@2x.webp",
            description = "难过",
            localPath = "emojis/emoji_16.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_19@2x.webp",
            description = "抓狂",
            localPath = "emojis/emoji_17.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_20@2x.webp",
            description = "吐",
            localPath = "emojis/emoji_18.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_21@2x.webp",
            description = "偷笑",
            localPath = "emojis/emoji_19.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_22@2x.webp",
            description = "可爱",
            localPath = "emojis/emoji_20.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_23@2x.webp",
            description = "白眼",
            localPath = "emojis/emoji_21.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_24@2x.webp",
            description = "傲慢",
            localPath = "emojis/emoji_22.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_26@2x.webp",
            description = "困",
            localPath = "emojis/emoji_23.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_27@2x.webp",
            description = "惊恐",
            localPath = "emojis/emoji_24.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_29@2x.webp",
            description = "憨笑",
            localPath = "emojis/emoji_25.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_30@2x.webp",
            description = "大兵",
            localPath = "emojis/emoji_26.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_32@2x.webp",
            description = "咒骂",
            localPath = "emojis/emoji_27.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_33@2x.webp",
            description = "疑问",
            localPath = "emojis/emoji_28.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_34@2x.webp",
            description = "嘘",
            localPath = "emojis/emoji_29.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_35@2x.webp",
            description = "晕",
            localPath = "emojis/emoji_30.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_37@2x.webp",
            description = "衰",
            localPath = "emojis/emoji_31.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_38@2x.webp",
            description = "骷髅",
            localPath = "emojis/emoji_32.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_39@2x.webp",
            description = "敲打",
            localPath = "emojis/emoji_33.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_40@2x.webp",
            description = "再见",
            localPath = "emojis/emoji_34.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_41@2x.webp",
            description = "擦汗",
            localPath = "emojis/emoji_35.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_42@2x.webp",
            description = "抠鼻",
            localPath = "emojis/emoji_36.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_43@2x.webp",
            description = "鼓掌",
            localPath = "emojis/emoji_37.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_45@2x.webp",
            description = "坏笑",
            localPath = "emojis/emoji_38.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_47@2x.webp",
            description = "右哼哼",
            localPath = "emojis/emoji_39.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_49@2x.webp",
            description = "鄙视",
            localPath = "emojis/emoji_40.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_50@2x.webp",
            description = "委屈",
            localPath = "emojis/emoji_41.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_51@2x.webp",
            description = "快哭了",
            localPath = "emojis/emoji_42.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_52@2x.webp",
            description = "阴险",
            localPath = "emojis/emoji_43.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_53@2x.webp",
            description = "亲亲",
            localPath = "emojis/emoji_44.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_55@2x.webp",
            description = "可怜",
            localPath = "emojis/emoji_45.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Happy.webp",
            description = "笑脸",
            localPath = "emojis/emoji_46.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Sick.webp",
            description = "生病",
            localPath = "emojis/emoji_47.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Flushed.webp",
            description = "脸红",
            localPath = "emojis/emoji_48.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Lol.webp",
            description = "破涕为笑",
            localPath = "emojis/emoji_49.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Terror.webp",
            description = "恐惧",
            localPath = "emojis/emoji_50.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/LetDown.webp",
            description = "失望",
            localPath = "emojis/emoji_51.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Duh.webp",
            description = "无语",
            localPath = "emojis/emoji_52.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_04.webp",
            description = "嘿哈",
            localPath = "emojis/emoji_53.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_05.webp",
            description = "捂脸",
            localPath = "emojis/emoji_54.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_02.webp",
            description = "奸笑",
            localPath = "emojis/emoji_55.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_06.webp",
            description = "机智",
            localPath = "emojis/emoji_56.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_12.webp",
            description = "皱眉",
            localPath = "emojis/emoji_57.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_11.webp",
            description = "耶",
            localPath = "emojis/emoji_58.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Watermelon.webp",
            description = "吃瓜",
            localPath = "emojis/emoji_59.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Addoil.webp",
            description = "加油",
            localPath = "emojis/emoji_60.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Sweat.webp",
            description = "汗",
            localPath = "emojis/emoji_61.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Shocked.webp",
            description = "天啊",
            localPath = "emojis/emoji_62.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Cold.webp",
            description = "Emm",
            localPath = "emojis/emoji_63.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Social.webp",
            description = "社会社会",
            localPath = "emojis/emoji_64.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Yellowdog.webp",
            description = "旺柴",
            localPath = "emojis/emoji_65.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/NoProb.webp",
            description = "好的",
            localPath = "emojis/emoji_66.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Slap.webp",
            description = "打脸",
            localPath = "emojis/emoji_67.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Wow.webp",
            description = "哇",
            localPath = "emojis/emoji_68.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Boring.webp",
            description = "翻白眼",
            localPath = "emojis/emoji_69.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/666.webp",
            description = "666",
            localPath = "emojis/emoji_70.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/LetMeSee.webp",
            description = "让我看看",
            localPath = "emojis/emoji_71.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Sigh.webp",
            description = "叹气",
            localPath = "emojis/emoji_72.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Hurt.webp",
            description = "苦涩",
            localPath = "emojis/emoji_73.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Broken.webp",
            description = "裂开",
            localPath = "emojis/emoji_74.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_66@2x.webp",
            description = "嘴唇",
            localPath = "emojis/emoji_75.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_67@2x.webp",
            description = "爱心",
            localPath = "emojis/emoji_76.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_68@2x.webp",
            description = "心碎",
            localPath = "emojis/emoji_77.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_79@2x.webp",
            description = "拥抱",
            localPath = "emojis/emoji_78.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_80@2x.webp",
            description = "强",
            localPath = "emojis/emoji_79.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_81@2x.webp",
            description = "弱",
            localPath = "emojis/emoji_80.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_82@2x.webp",
            description = "握手",
            localPath = "emojis/emoji_81.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_83@2x.webp",
            description = "胜利",
            localPath = "emojis/emoji_82.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_84@2x.webp",
            description = "抱拳",
            localPath = "emojis/emoji_83.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_85@2x.webp",
            description = "勾引",
            localPath = "emojis/emoji_84.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_86@2x.webp",
            description = "拳头",
            localPath = "emojis/emoji_85.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_90@2x.webp",
            description = "OK",
            localPath = "emojis/emoji_86.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Worship.webp",
            description = "合十",
            localPath = "emojis/emoji_87.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_58@2x.webp",
            description = "啤酒",
            localPath = "emojis/emoji_88.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_61@2x.webp",
            description = "咖啡",
            localPath = "emojis/emoji_89.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_69@2x.webp",
            description = "蛋糕",
            localPath = "emojis/emoji_90.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_64@2x.webp",
            description = "玫瑰",
            localPath = "emojis/emoji_91.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_65@2x.webp",
            description = "凋谢",
            localPath = "emojis/emoji_92.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_56@2x.webp",
            description = "菜刀",
            localPath = "emojis/emoji_93.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_71@2x.webp",
            description = "炸弹",
            localPath = "emojis/emoji_94.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_75@2x.webp",
            description = "便便",
            localPath = "emojis/emoji_95.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_76@2x.webp",
            description = "月亮",
            localPath = "emojis/emoji_96.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_77@2x.webp",
            description = "太阳",
            localPath = "emojis/emoji_97.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Party.webp",
            description = "庆祝",
            localPath = "emojis/emoji_98.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_78@2x.webp",
            description = "礼物",
            localPath = "emojis/emoji_99.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_09.webp",
            description = "红包",
            localPath = "emojis/emoji_100.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_16.webp",
            description = "發",
            localPath = "emojis/emoji_101.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/2_15.webp",
            description = "福",
            localPath = "emojis/emoji_102.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Fireworks.webp",
            description = "烟花",
            localPath = "emojis/emoji_103.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/newemoji/Firecracker.webp",
            description = "爆竹",
            localPath = "emojis/emoji_104.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_63@2x.webp",
            description = "猪头",
            localPath = "emojis/emoji_105.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_93@2x.webp",
            description = "跳跳",
            localPath = "emojis/emoji_106.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_94@2x.webp",
            description = "发抖",
            localPath = "emojis/emoji_107.webp"
        ), Emoji(
            remoteUrl = "https://res.wx.qq.com/t/wx_fed/we-emoji/res/v1.2.8/assets/Expression/Expression_96@2x.webp",
            description = "转圈",
            localPath = "emojis/emoji_108.webp"
        )
    )

    // 静态查找映射
    private val descriptionMap by lazy {
        all.associateBy { it.description }
    }

    // 根据描述查找
    fun findByDescription(description: String): Emoji? {
        return descriptionMap[description]
    }

    // 模式匹配
    private val PATTERN_REGEX = Regex("\\[([^]]+)]")

    fun findAllMatches(text: CharSequence): List<EmojiMatch> {
        return PATTERN_REGEX.findAll(text).mapNotNull { match ->
            val desc = match.groupValues[1]
            val emoji = findByDescription(desc)
            if (emoji != null) EmojiMatch(emoji, match.range) else null
        }.toList()
    }
}

data class EmojiMatch(val emoji: Emoji, val range: IntRange)