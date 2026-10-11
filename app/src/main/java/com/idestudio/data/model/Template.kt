package com.idestudio.data.model

data class Template(
    val id: String,
    val name: String,
    val description: String,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val iconName: String = "simple"
) {
    companion object {
        val ALL_TEMPLATES = listOf(
            Template(
                id = "simple_app",
                name = "Simple App",
                description = "Clean single-screen Android application with standard activity and interactive layout.",
                minSdk = 21,
                targetSdk = 34,
                iconName = "simple"
            ),
            Template(
                id = "fullscreen_app",
                name = "Full Screen App",
                description = "Immersive full-screen application layout with edge-to-edge view and hidden status bar.",
                minSdk = 21,
                targetSdk = 34,
                iconName = "fullscreen"
            ),
            Template(
                id = "navigation_drawer_app",
                name = "Navigation Drawer App",
                description = "Structured Android application with slide-out side navigation drawer and menu sections.",
                minSdk = 21,
                targetSdk = 34,
                iconName = "drawer"
            ),
            Template(
                id = "basic_activity",
                name = "Basic Activity",
                description = "Standard Android activity featuring top header toolbar and interactive action controls.",
                minSdk = 21,
                targetSdk = 34,
                iconName = "basic"
            ),
            Template(
                id = "empty_activity",
                name = "Empty Activity",
                description = "Minimal baseline Android activity template designed for clean, customized development.",
                minSdk = 21,
                targetSdk = 34,
                iconName = "empty"
            )
        )
    }
}
