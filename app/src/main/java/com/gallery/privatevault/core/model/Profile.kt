package com.gallery.privatevault.core.model

/**
 * Modelo de Perfil de usuario para visibilidad y aislamiento de contenidos.
 */
data class Profile(
    val id: Long = 0,
    val name: String,
    val iconName: String = "folder",
    val isProtected: Boolean = false,
    val pinHash: String? = null,
    val isReadOnly: Boolean = false,
    val linkedThemeId: String? = null,
    val includeAllByDefault: Boolean = true,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val rules: List<FolderRule> = emptyList(),
    val virtualFolderIds: Set<Long> = emptySet()
)
