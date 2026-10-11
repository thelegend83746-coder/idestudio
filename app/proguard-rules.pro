# ProGuard rules for IDE STUDIO
-keepattributes SourceFile,LineNumberTable
-keep class com.idestudio.data.model.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
