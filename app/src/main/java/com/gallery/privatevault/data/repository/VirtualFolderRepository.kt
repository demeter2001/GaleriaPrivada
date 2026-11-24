package com.gallery.privatevault.data.repository

import com.gallery.privatevault.core.database.dao.VirtualFolderDao
import com.gallery.privatevault.core.database.entity.VirtualFolderEntity
import com.gallery.privatevault.core.database.entity.VirtualFolderMediaCrossRef
import com.gallery.privatevault.core.model.VirtualFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class VirtualFolderRepository(
    private val virtualFolderDao: VirtualFolderDao,
    private val profileRepository: ProfileRepository
) {

    val activeProfileVirtualFolders: Flow<List<VirtualFolder>> = profileRepository.activeProfileId
        .flatMapLatest { profileId ->
            virtualFolderDao.getVirtualFoldersForProfileFlow(profileId)
        }
        .map { list ->
            list.map { entity ->
                VirtualFolder(
                    id = entity.id,
                    name = entity.name,
                    iconName = entity.iconName,
                    coverUri = entity.coverUri,
                    sortOrder = entity.sortOrder,
                    columnCount = entity.columnCount,
                    isPinned = entity.isPinned,
                    createdAt = entity.createdAt
                )
            }
        }

    val pinnedFolders: Flow<List<VirtualFolder>> = virtualFolderDao.getPinnedVirtualFoldersFlow()
        .map { list ->
            list.map { entity ->
                VirtualFolder(
                    id = entity.id,
                    name = entity.name,
                    iconName = entity.iconName,
                    coverUri = entity.coverUri,
                    sortOrder = entity.sortOrder,
                    columnCount = entity.columnCount,
                    isPinned = entity.isPinned,
                    createdAt = entity.createdAt
                )
            }
        }

    suspend fun createVirtualFolder(name: String, iconName: String = "folder_special"): Long {
        return virtualFolderDao.insertVirtualFolder(
            VirtualFolderEntity(
                name = name,
                iconName = iconName
            )
        )
    }

    suspend fun addMediaToFolder(folderId: Long, mediaStoreId: Long, mediaUriString: String) {
        virtualFolderDao.addMediaToVirtualFolder(
            VirtualFolderMediaCrossRef(
                virtualFolderId = folderId,
                mediaStoreId = mediaStoreId,
                mediaUriString = mediaUriString
            )
        )
    }

    suspend fun removeMediaFromFolder(folderId: Long, mediaStoreId: Long) {
        virtualFolderDao.removeMediaFromVirtualFolder(folderId, mediaStoreId)
    }
}
