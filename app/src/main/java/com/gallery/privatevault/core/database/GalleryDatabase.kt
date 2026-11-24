package com.gallery.privatevault.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gallery.privatevault.core.database.converter.Converters
import com.gallery.privatevault.core.database.dao.AppSettingsDao
import com.gallery.privatevault.core.database.dao.MediaMetadataDao
import com.gallery.privatevault.core.database.dao.ProfileDao
import com.gallery.privatevault.core.database.dao.ThemeDao
import com.gallery.privatevault.core.database.dao.VirtualFolderDao
import com.gallery.privatevault.core.database.entity.AppSettingsEntity
import com.gallery.privatevault.core.database.entity.MediaMetadataEntity
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.database.entity.ProfileFolderRuleEntity
import com.gallery.privatevault.core.database.entity.ProfileVirtualFolderCrossRef
import com.gallery.privatevault.core.database.entity.ThemeEntity
import com.gallery.privatevault.core.database.entity.VirtualFolderEntity
import com.gallery.privatevault.core.database.entity.VirtualFolderMediaCrossRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        ProfileFolderRuleEntity::class,
        VirtualFolderEntity::class,
        ProfileVirtualFolderCrossRef::class,
        VirtualFolderMediaCrossRef::class,
        MediaMetadataEntity::class,
        ThemeEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun virtualFolderDao(): VirtualFolderDao
    abstract fun themeDao(): ThemeDao
    abstract fun mediaMetadataDao(): MediaMetadataDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GalleryDatabase::class.java,
                    "private_gallery_database.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultData(database)
                }
            }
        }

        private suspend fun seedDefaultData(db: GalleryDatabase) {
            // 1. Perfil Inicial por defecto: General
            val defaultProfileId = db.profileDao().insertProfile(
                ProfileEntity(
                    id = 1,
                    name = "General",
                    iconName = "dashboard",
                    isProtected = false,
                    isReadOnly = false,
                    linkedThemeId = "amoled",
                    includeAllByDefault = true,
                    isDefault = true,
                    orderIndex = 0
                )
            )

            // Perfil Secundario de ejemplo: Privado (bloqueado por biometría/PIN)
            db.profileDao().insertProfile(
                ProfileEntity(
                    id = 2,
                    name = "Privado",
                    iconName = "lock",
                    isProtected = true,
                    isReadOnly = false,
                    linkedThemeId = "amoled",
                    includeAllByDefault = false,
                    isDefault = false,
                    orderIndex = 1
                )
            )

            // 2. Temas Preinstalados (Claro, Oscuro, Negro Puro AMOLED, Sepia, Alto Contraste, Minimalista)
            val builtInThemes = listOf(
                ThemeEntity(
                    id = "amoled",
                    name = "Negro Puro AMOLED",
                    isBuiltIn = true,
                    primaryColorHex = "#64B5F6",
                    onPrimaryColorHex = "#000000",
                    surfaceColorHex = "#121212",
                    onSurfaceColorHex = "#FFFFFF",
                    backgroundColorHex = "#000000",
                    onBackgroundColorHex = "#EEEEEE",
                    accentColorHex = "#82B1FF",
                    cardCornerRadiusDp = 18,
                    gridDensityColumns = 3,
                    barAlpha = 0.94f,
                    barBlurEnabled = true,
                    isDark = true
                ),
                ThemeEntity(
                    id = "dark_standard",
                    name = "Oscuro Estándar",
                    isBuiltIn = true,
                    primaryColorHex = "#90CAF9",
                    onPrimaryColorHex = "#0D47A1",
                    surfaceColorHex = "#1E1E1E",
                    onSurfaceColorHex = "#E0E0E0",
                    backgroundColorHex = "#121212",
                    onBackgroundColorHex = "#F5F5F5",
                    accentColorHex = "#448AFF",
                    cardCornerRadiusDp = 16,
                    gridDensityColumns = 3,
                    barAlpha = 0.90f,
                    barBlurEnabled = true,
                    isDark = true
                ),
                ThemeEntity(
                    id = "light_standard",
                    name = "Claro Estándar",
                    isBuiltIn = true,
                    primaryColorHex = "#1976D2",
                    onPrimaryColorHex = "#FFFFFF",
                    surfaceColorHex = "#F5F5F5",
                    onSurfaceColorHex = "#212121",
                    backgroundColorHex = "#FFFFFF",
                    onBackgroundColorHex = "#1C1B1F",
                    accentColorHex = "#2979FF",
                    cardCornerRadiusDp = 16,
                    gridDensityColumns = 3,
                    barAlpha = 0.95f,
                    barBlurEnabled = true,
                    isDark = false
                ),
                ThemeEntity(
                    id = "sepia",
                    name = "Sepia Cálido",
                    isBuiltIn = true,
                    primaryColorHex = "#8D6E63",
                    onPrimaryColorHex = "#FFFFFF",
                    surfaceColorHex = "#EFEBE9",
                    onSurfaceColorHex = "#3E2723",
                    backgroundColorHex = "#F5F0EB",
                    onBackgroundColorHex = "#2B1B17",
                    accentColorHex = "#A1887F",
                    cardCornerRadiusDp = 14,
                    gridDensityColumns = 3,
                    barAlpha = 0.92f,
                    barBlurEnabled = false,
                    isDark = false
                ),
                ThemeEntity(
                    id = "high_contrast",
                    name = "Alto Contraste",
                    isBuiltIn = true,
                    primaryColorHex = "#FFEB3B",
                    onPrimaryColorHex = "#000000",
                    surfaceColorHex = "#1A1A1A",
                    onSurfaceColorHex = "#FFFFFF",
                    backgroundColorHex = "#000000",
                    onBackgroundColorHex = "#FFFFFF",
                    accentColorHex = "#00E676",
                    cardCornerRadiusDp = 8,
                    gridDensityColumns = 3,
                    barAlpha = 1.0f,
                    barBlurEnabled = false,
                    isDark = true
                ),
                ThemeEntity(
                    id = "minimalist",
                    name = "Minimalista",
                    isBuiltIn = true,
                    primaryColorHex = "#B0BEC5",
                    onPrimaryColorHex = "#263238",
                    surfaceColorHex = "#181A1B",
                    onSurfaceColorHex = "#ECEFF1",
                    backgroundColorHex = "#0E1012",
                    onBackgroundColorHex = "#ECEFF1",
                    accentColorHex = "#78909C",
                    cardCornerRadiusDp = 20,
                    gridDensityColumns = 4,
                    barAlpha = 0.85f,
                    barBlurEnabled = true,
                    isDark = true
                )
            )
            db.themeDao().insertThemes(builtInThemes)

            // 3. Ajustes iniciales de la app
            db.appSettingsDao().upsertSettings(
                AppSettingsEntity(
                    id = 1,
                    activeProfileId = defaultProfileId,
                    activeThemeId = "amoled"
                )
            )
        }
    }
}
