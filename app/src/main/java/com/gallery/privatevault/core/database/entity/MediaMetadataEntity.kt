package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_metadata_overrides",
    indices = [
        Index("isFavorite"),
        Index("isTrash")
    ]
)
data class MediaMetadataEntity(
    @PrimaryKey
    val mediaStoreId: Long,
    val mediaUriString: String,
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val tagsJson: String = "[]",
    val isTrash: Boolean = false,
    val trashTimestamp: Long? = null,
    val customNotes: String? = null
)
