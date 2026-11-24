package com.gallery.privatevault.data.repository

import com.gallery.privatevault.core.database.dao.AppSettingsDao
import com.gallery.privatevault.core.database.dao.ThemeDao
import com.gallery.privatevault.core.database.entity.ThemeEntity
import com.gallery.privatevault.core.model.ThemeConfig
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ThemeRepository(
    private val themeDao: ThemeDao,
    private val appSettingsDao: AppSettingsDao,
    private val profileRepository: ProfileRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Resuelve el tema activo reactivamente:
     * Si el perfil activo tiene un tema vinculado (`linkedThemeId`), se utiliza dicho tema.
     * En caso contrario, se utiliza el tema global configurado en `AppSettings`.
     */
    val activeThemeConfig: Flow<ThemeConfig> = combine(
        profileRepository.activeProfileWithDetails,
        appSettingsDao.getSettingsFlow()
    ) { profileDetails, settings ->
        val targetThemeId = profileDetails?.profile?.linkedThemeId ?: settings?.activeThemeId ?: "amoled"
        targetThemeId
    }.flatMapLatest { themeId ->
        themeDao.getThemeByIdFlow(themeId)
    }.map { entity ->
        entity?.toDomain() ?: defaultThemeConfig
    }

    val allThemes: Flow<List<ThemeConfig>> = themeDao.getAllThemesFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun setActiveTheme(themeId: String) {
        appSettingsDao.updateActiveTheme(themeId)
    }

    suspend fun saveTheme(config: ThemeConfig) {
        themeDao.insertTheme(config.toEntity())
    }

    suspend fun deleteCustomTheme(themeId: String) {
        themeDao.deleteCustomTheme(themeId)
    }

    /**
     * Exporta la configuración de un tema a texto JSON portátil.
     */
    fun exportThemeToJson(theme: ThemeConfig): String {
        return json.encodeToString(theme)
    }

    /**
     * Importa una configuración de tema desde un archivo o cadena JSON.
     */
    fun importThemeFromJson(jsonString: String): Result<ThemeConfig> {
        return runCatching {
            val config = json.decodeFromString<ThemeConfig>(jsonString)
            // Asegurar que no sobreescriba builtIn protegido con id colisionado
            val safeId = if (config.isBuiltIn) "custom_${System.currentTimeMillis()}" else config.id
            config.copy(id = safeId, isBuiltIn = false)
        }
    }

    private fun ThemeEntity.toDomain(): ThemeConfig = ThemeConfig(
        id = id,
        name = name,
        isBuiltIn = isBuiltIn,
        primaryColorHex = primaryColorHex,
        onPrimaryColorHex = onPrimaryColorHex,
        surfaceColorHex = surfaceColorHex,
        onSurfaceColorHex = onSurfaceColorHex,
        backgroundColorHex = backgroundColorHex,
        onBackgroundColorHex = onBackgroundColorHex,
        accentColorHex = accentColorHex,
        cardCornerRadiusDp = cardCornerRadiusDp,
        gridDensityColumns = gridDensityColumns,
        barAlpha = barAlpha,
        barBlurEnabled = barBlurEnabled,
        backgroundMediaUri = backgroundMediaUri,
        backgroundBlurRadius = backgroundBlurRadius,
        backgroundAlpha = backgroundAlpha,
        isDark = isDark
    )

    private fun ThemeConfig.toEntity(): ThemeEntity = ThemeEntity(
        id = id,
        name = name,
        isBuiltIn = isBuiltIn,
        primaryColorHex = primaryColorHex,
        onPrimaryColorHex = onPrimaryColorHex,
        surfaceColorHex = surfaceColorHex,
        onSurfaceColorHex = onSurfaceColorHex,
        backgroundColorHex = backgroundColorHex,
        onBackgroundColorHex = onBackgroundColorHex,
        accentColorHex = accentColorHex,
        cardCornerRadiusDp = cardCornerRadiusDp,
        gridDensityColumns = gridDensityColumns,
        barAlpha = barAlpha,
        barBlurEnabled = barBlurEnabled,
        backgroundMediaUri = backgroundMediaUri,
        backgroundBlurRadius = backgroundBlurRadius,
        backgroundAlpha = backgroundAlpha,
        isDark = isDark
    )

    companion object {
        val defaultThemeConfig = ThemeConfig(
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
        )
    }
}
