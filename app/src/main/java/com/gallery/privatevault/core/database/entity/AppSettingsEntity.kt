package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gallery.privatevault.core.model.NavigationStyle

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val activeProfileId: Long = 1L,
    val navigationStyle: NavigationStyle = NavigationStyle.BOTTOM_BAR,
    val trashRetentionDays: Int = 30,
    val autoMaxBrightness: Boolean = false,
    val slideshowIntervalSeconds: Int = 3,
    val activeThemeId: String = "amoled"
)
