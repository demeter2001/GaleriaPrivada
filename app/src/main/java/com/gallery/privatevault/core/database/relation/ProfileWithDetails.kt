package com.gallery.privatevault.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.database.entity.ProfileFolderRuleEntity
import com.gallery.privatevault.core.database.entity.ProfileVirtualFolderCrossRef
import com.gallery.privatevault.core.database.entity.VirtualFolderEntity

data class ProfileWithDetails(
    @Embedded
    val profile: ProfileEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val rules: List<ProfileFolderRuleEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ProfileVirtualFolderCrossRef::class,
            parentColumn = "profileId",
            entityColumn = "virtualFolderId"
        )
    )
    val virtualFolders: List<VirtualFolderEntity>
)
