# Keep generated kotlinx.serialization serializers for backup models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.kelsuita.starswish.data.**$$serializer { *; }
-keepclassmembers class com.kelsuita.starswish.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.kelsuita.starswish.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
