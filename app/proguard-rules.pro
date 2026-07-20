# The watch installs over ADB Wi-Fi, which is slow enough that APK size is a
# real constraint. R8 takes the debug build's ~24 MB down to a few MB, mostly by
# dropping unused Compose material and tooling.

# kotlinx.serialization generates serializers reflectively looked up by name.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.smz70.mmhue.watch.** {
    *** Companion;
}
-keepclasseswithmembers class com.smz70.mmhue.watch.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.smz70.mmhue.watch.**$$serializer { *; }

# OkHttp ships its own consumer rules; these silence the optional-dependency
# warnings it produces on Android, where those classes are never present.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
