package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "profile_virtual_folder_cross_ref",
    primaryKeys = ["profileId", "virtualFolderId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VirtualFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["virtualFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("virtualFolderId")
    ]
)
data class ProfileVirtualFolderCrossRef(
    val profileId: Long,
    val virtualFolderId: Long
)
