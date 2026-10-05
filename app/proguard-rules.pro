# IDE STUDIO Proguard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep data models
-keep class com.idestudio.app.data.model.** { *; }
