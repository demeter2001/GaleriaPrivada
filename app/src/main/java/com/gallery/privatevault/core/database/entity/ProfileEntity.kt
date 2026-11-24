package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
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
    val createdAt: Long = System.currentTimeMillis()
)
