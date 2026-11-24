package com.gallery.privatevault.core.database.dao

import androidx.room.*
import com.gallery.privatevault.core.database.entity.MediaMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaMetadataDao {

    @Query("SELECT * FROM media_metadata_overrides WHERE mediaStoreId = :id")
    fun getMetadataFlow(id: Long): Flow<MediaMetadataEntity?>

    @Query("SELECT * FROM media_metadata_overrides WHERE mediaStoreId = :id")
    suspend fun getMetadata(id: Long): MediaMetadataEntity?

    @Query("SELECT * FROM media_metadata_overrides WHERE isFavorite = 1 AND isTrash = 0")
    fun getFavoriteMetadataFlow(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media_metadata_overrides WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashMediaFlow(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media_metadata_overrides")
    fun getAllMetadataFlow(): Flow<List<MediaMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: MediaMetadataEntity)

    @Query("UPDATE media_metadata_overrides SET isFavorite = :isFavorite WHERE mediaStoreId = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_metadata_overrides SET isTrash = 1, trashTimestamp = :timestamp WHERE mediaStoreId = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_metadata_overrides SET isTrash = 0, trashTimestamp = NULL WHERE mediaStoreId = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM media_metadata_overrides WHERE isTrash = 1 AND trashTimestamp < :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long): Int
}
