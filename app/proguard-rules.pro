# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# AMap
-keep class com.autonavi.**{ *; }
-keep class com.amap.api.**.**{ *; }
-keep, allowobfuscation class top.chengdongqing.wechat.core.designsystem.components.location.model.**

# Pinyin
-keep,allowobfuscation class com.github.houbb.pinyin.** { *; }

# MLKit
-keepclassmembers class com.google.mlkit.** {
    native <methods>;
    public <init>(...);
}
-keep enum com.google.mlkit.**  { *; }
-keep interface com.google.mlkit.**  { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-keepattributes InnerClasses
-keepattributes *Annotation*
-keepattributes Exceptions
-keep class org.jni_zero.** { *; }
-keepnames class org.jni_zero.** { *; }
-keepclasseswithmembernames class * {
    @org.jni_zero.NativeMethods *;
}

# 保持异常堆栈的行号和源文件名，方便排查崩溃
#-keepattributes SourceFile,LineNumberTable
# 强制保留某些可能用于反射的类名，让报错信息显示出原始类名
#-keepattributes Signature,EnclosingMethod,InnerClasses,AnnotationDefault