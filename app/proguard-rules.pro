# Release builds are not minified by default (isMinifyEnabled = false).
# If you enable R8, keep the serializable backup models:
-keepclassmembers class com.liferpg.adventure.** { *** Companion; }
-keep,includedescriptorclasses class com.liferpg.adventure.**$$serializer { *; }
