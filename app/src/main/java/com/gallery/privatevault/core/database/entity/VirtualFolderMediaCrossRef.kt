package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "virtual_folder_media_cross_ref",
    primaryKeys = ["virtualFolderId", "mediaStoreId"],
    foreignKeys = [
        ForeignKey(
            entity = VirtualFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["virtualFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("virtualFolderId"),
        Index("mediaStoreId")
    ]
)
data class VirtualFolderMediaCrossRef(
    val virtualFolderId: Long,
    val mediaStoreId: Long,
    val mediaUriString: String,
    val addedAt: Long = System.currentTimeMillis()
)
