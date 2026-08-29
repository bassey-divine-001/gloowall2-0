# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\USER\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt

# Keep accessibility service
-keep class com.gloowalltapper.AccessibilityService { *; }

# Keep Google Play services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
