# Add project specific ProGuard rules here.
-keep class com.binbashmedium.sightreadingtrainer.domain.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}
