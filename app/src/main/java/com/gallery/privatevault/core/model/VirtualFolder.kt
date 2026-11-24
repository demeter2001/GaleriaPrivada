package com.gallery.privatevault.core.model

enum class FolderSortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC
}

/**
 * Carpeta Virtual No Destructiva.
 * Vive únicamente en Room sin mover ni copiar archivos físicos.
 */
data class VirtualFolder(
    val id: Long = 0,
    val name: String,
    val iconName: String = "folder_special",
    val coverUri: String? = null,
    val sortOrder: FolderSortOrder = FolderSortOrder.DATE_DESC,
    val columnCount: Int = 3,
    val isPinned: Boolean = false,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
