# Placeta Junior ProGuard Rules
-keep class org.laplaceta.placetajunior.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
