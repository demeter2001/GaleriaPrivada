package com.gallery.privatevault.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gallery.privatevault.core.model.RuleType

@Entity(
    tableName = "profile_folder_rules",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "folderPath"], unique = true)
    ]
)
data class ProfileFolderRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val folderPath: String,
    val ruleType: RuleType,
    val isRecursive: Boolean = true
)
