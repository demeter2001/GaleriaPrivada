package com.gallery.privatevault.core.model

enum class NavigationStyle {
    BOTTOM_BAR,
    DRAWER,
    HYBRID
}

data class AppSettings(
    val activeProfileId: Long = 1L,
    val navigationStyle: NavigationStyle = NavigationStyle.BOTTOM_BAR,
    val trashRetentionDays: Int = 30,
    val autoMaxBrightness: Boolean = false,
    val slideshowIntervalSeconds: Int = 3,
    val activeThemeId: String = "amoled"
)
