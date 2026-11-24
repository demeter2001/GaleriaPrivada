package com.gallery.privatevault.core.database.dao

import androidx.room.*
import com.gallery.privatevault.core.database.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Query("UPDATE app_settings SET activeProfileId = :profileId WHERE id = 1")
    suspend fun updateActiveProfile(profileId: Long)

    @Query("UPDATE app_settings SET activeThemeId = :themeId WHERE id = 1")
    suspend fun updateActiveTheme(themeId: String)

    @Query("UPDATE app_settings SET navigationStyle = :style WHERE id = 1")
    suspend fun updateNavigationStyle(style: com.gallery.privatevault.core.model.NavigationStyle)

    @Query("UPDATE app_settings SET trashRetentionDays = :days WHERE id = 1")
    suspend fun updateTrashRetentionDays(days: Int)

    @Query("UPDATE app_settings SET autoMaxBrightness = :enabled WHERE id = 1")
    suspend fun updateAutoMaxBrightness(enabled: Boolean)
}
