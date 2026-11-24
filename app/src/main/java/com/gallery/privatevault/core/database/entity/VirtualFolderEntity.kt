package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gallery.privatevault.core.model.FolderSortOrder

@Entity(tableName = "virtual_folders")
data class VirtualFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "folder_special",
    val coverUri: String? = null,
    val sortOrder: FolderSortOrder = FolderSortOrder.DATE_DESC,
    val columnCount: Int = 3,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
