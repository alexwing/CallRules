# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.icaria.callrules.**$$serializer { *; }
-keepclassmembers class com.icaria.callrules.** {
    *** Companion;
}
-keepclasseswithmembers class com.icaria.callrules.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# libphonenumber carga sus metadatos como recursos del jar
-dontwarn com.google.i18n.phonenumbers.**
