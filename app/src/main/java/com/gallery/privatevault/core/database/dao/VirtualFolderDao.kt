package com.gallery.privatevault.core.database.dao

import androidx.room.*
import com.gallery.privatevault.core.database.entity.VirtualFolderEntity
import com.gallery.privatevault.core.database.entity.VirtualFolderMediaCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualFolderDao {

    @Query("SELECT * FROM virtual_folders ORDER BY isPinned DESC, createdAt DESC")
    fun getAllVirtualFoldersFlow(): Flow<List<VirtualFolderEntity>>

    @Query("SELECT * FROM virtual_folders WHERE isPinned = 1 ORDER BY createdAt DESC")
    fun getPinnedVirtualFoldersFlow(): Flow<List<VirtualFolderEntity>>

    @Query("""
        SELECT vf.* FROM virtual_folders vf
        INNER JOIN profile_virtual_folder_cross_ref pvf ON vf.id = pvf.virtualFolderId
        WHERE pvf.profileId = :profileId
        ORDER BY vf.isPinned DESC, vf.createdAt DESC
    """)
    fun getVirtualFoldersForProfileFlow(profileId: Long): Flow<List<VirtualFolderEntity>>

    @Query("SELECT * FROM virtual_folders WHERE id = :folderId")
    suspend fun getVirtualFolderById(folderId: Long): VirtualFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualFolder(folder: VirtualFolderEntity): Long

    @Update
    suspend fun updateVirtualFolder(folder: VirtualFolderEntity)

    @Delete
    suspend fun deleteVirtualFolder(folder: VirtualFolderEntity)

    // Enlaces de medios a carpetas virtuales
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMediaToVirtualFolder(crossRef: VirtualFolderMediaCrossRef)

    @Query("DELETE FROM virtual_folder_media_cross_ref WHERE virtualFolderId = :folderId AND mediaStoreId = :mediaStoreId")
    suspend fun removeMediaFromVirtualFolder(folderId: Long, mediaStoreId: Long)

    @Query("SELECT mediaStoreId FROM virtual_folder_media_cross_ref WHERE virtualFolderId = :folderId ORDER BY addedAt DESC")
    fun getMediaStoreIdsInFolderFlow(folderId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM virtual_folder_media_cross_ref WHERE virtualFolderId = :folderId")
    fun countMediaInFolderFlow(folderId: Long): Flow<Int>
}
