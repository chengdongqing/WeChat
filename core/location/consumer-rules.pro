-keep class com.autonavi.**{ *; }
-keep class com.amap.api.**{ *; }
-keep, allowobfuscation class top.chengdongqing.wechat.core.location.model.**

-dontwarn com.amap.ams.gnss.**
-dontwarn net.jafama.**
-keep class com.amap.ams.gnss.** { *; }
-keep class net.jafama.** { *; }