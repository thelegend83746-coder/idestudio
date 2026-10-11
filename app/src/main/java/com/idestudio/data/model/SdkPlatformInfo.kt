package com.idestudio.data.model

enum class SdkStatus {
    INSTALLED,
    MISSING,
    UNSUPPORTED,
    READY
}

data class SdkPlatformInfo(
    val apiLevel: Int,
    val versionName: String,
    val codeName: String,
    val androidJarPath: String? = null,
    val status: SdkStatus = SdkStatus.MISSING
) {
    val isUsable: Boolean
        get() = status == SdkStatus.INSTALLED || status == SdkStatus.READY

    companion object {
        val ALL_KNOWN_PLATFORMS = listOf(
            SdkPlatformInfo(apiLevel = 16, versionName = "4.1", codeName = "Jelly Bean", status = SdkStatus.UNSUPPORTED),
            SdkPlatformInfo(apiLevel = 19, versionName = "4.4", codeName = "KitKat", status = SdkStatus.UNSUPPORTED),
            SdkPlatformInfo(apiLevel = 21, versionName = "5.0", codeName = "Lollipop"),
            SdkPlatformInfo(apiLevel = 23, versionName = "6.0", codeName = "Marshmallow"),
            SdkPlatformInfo(apiLevel = 24, versionName = "7.0", codeName = "Nougat"),
            SdkPlatformInfo(apiLevel = 26, versionName = "8.0", codeName = "Oreo"),
            SdkPlatformInfo(apiLevel = 28, versionName = "9.0", codeName = "Pie"),
            SdkPlatformInfo(apiLevel = 29, versionName = "10.0", codeName = "Android 10"),
            SdkPlatformInfo(apiLevel = 30, versionName = "11.0", codeName = "Android 11"),
            SdkPlatformInfo(apiLevel = 31, versionName = "12.0", codeName = "Android 12"),
            SdkPlatformInfo(apiLevel = 32, versionName = "12L", codeName = "Android 12L"),
            SdkPlatformInfo(apiLevel = 33, versionName = "13.0", codeName = "Android 13"),
            SdkPlatformInfo(apiLevel = 34, versionName = "14.0", codeName = "Android 14"),
            SdkPlatformInfo(apiLevel = 35, versionName = "15.0", codeName = "Android 15"),
            SdkPlatformInfo(apiLevel = 36, versionName = "16.0", codeName = "Android 16")
        )
    }
}
