-keepclassmembers class com.google.mlkit.** {
    native <methods>;
    public <init>(...);
}
-keep enum com.google.mlkit.**  { *; }
-keep interface com.google.mlkit.**  { *; }