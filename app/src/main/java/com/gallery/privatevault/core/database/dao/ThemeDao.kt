package com.gallery.privatevault.core.database.dao

import androidx.room.*
import com.gallery.privatevault.core.database.entity.ThemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {

    @Query("SELECT * FROM themes ORDER BY isBuiltIn DESC, name ASC")
    fun getAllThemesFlow(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :themeId")
    fun getThemeByIdFlow(themeId: String): Flow<ThemeEntity?>

    @Query("SELECT * FROM themes WHERE id = :themeId")
    suspend fun getThemeById(themeId: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThemes(themes: List<ThemeEntity>)

    @Query("DELETE FROM themes WHERE id = :themeId AND isBuiltIn = 0")
    suspend fun deleteCustomTheme(themeId: String)
}
