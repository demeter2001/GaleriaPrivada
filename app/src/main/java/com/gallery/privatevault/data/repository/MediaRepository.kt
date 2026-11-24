package com.gallery.privatevault.data.repository

import com.gallery.privatevault.core.database.dao.MediaMetadataDao
import com.gallery.privatevault.core.database.entity.MediaMetadataEntity
import com.gallery.privatevault.core.mediastore.MediaStoreDataSource
import com.gallery.privatevault.core.mediastore.ProfileRuleEvaluator
import com.gallery.privatevault.core.model.MediaItem
import kotlinx.coroutines.flow.*

class MediaRepository(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val profileRepository: ProfileRepository,
    private val mediaMetadataDao: MediaMetadataDao
) {

    /**
     * Flujo reactivo de todos los medios permitidos bajo las reglas del perfil activo.
     */
    val permittedMediaFlow: Flow<List<MediaItem>> = combine(
        mediaStoreDataSource.mediaChangesFlow,
        profileRepository.activeProfileEvaluator,
        mediaMetadataDao.getAllMetadataFlow()
    ) { _, evaluator, overrides ->
        val rawMedia = mediaStoreDataSource.fetchAllMedia()
        val overrideMap = overrides.associateBy { it.mediaStoreId }

        rawMedia
            .map { item ->
                val ovr = overrideMap[item.id]
                if (ovr != null) {
                    item.copy(
                        isFavorite = ovr.isFavorite,
                        rating = ovr.rating,
                        isTrash = ovr.isTrash
                    )
                } else {
                    item
                }
            }
            .filter { item -> evaluator.isMediaPermitted(item) }
    }

    /**
     * Imágenes recientes permitidas para el bloque de Inicio.
     */
    fun getRecentImages(limit: Int = 20): Flow<List<MediaItem>> = permittedMediaFlow.map { list ->
        list.asSequence()
            .filter { !it.isVideo }
            .take(limit)
            .toList()
    }

    /**
     * Videos recientes permitidos para el bloque de Inicio.
     */
    fun getRecentVideos(limit: Int = 20): Flow<List<MediaItem>> = permittedMediaFlow.map { list ->
        list.asSequence()
            .filter { it.isVideo }
            .take(limit)
            .toList()
    }

    /**
     * Búsqueda en el catálogo permitido por nombre, carpeta o etiquetas.
     */
    fun searchPermittedMedia(query: String): Flow<List<MediaItem>> = permittedMediaFlow.map { list ->
        if (query.isBlank()) return@map list
        val q = query.trim().lowercase()
        list.filter { item ->
            item.displayName.lowercase().contains(q) ||
            item.bucketDisplayName.lowercase().contains(q) ||
            item.tags.any { it.lowercase().contains(q) }
        }
    }

    suspend fun toggleFavorite(mediaStoreId: Long, uriString: String, currentFavorite: Boolean) {
        val existing = mediaMetadataDao.getMetadata(mediaStoreId)
        if (existing == null) {
            mediaMetadataDao.upsertMetadata(
                MediaMetadataEntity(
                    mediaStoreId = mediaStoreId,
                    mediaUriString = uriString,
                    isFavorite = !currentFavorite
                )
            )
        } else {
            mediaMetadataDao.setFavorite(mediaStoreId, !currentFavorite)
        }
    }

    suspend fun moveToTrash(mediaStoreId: Long, uriString: String) {
        val existing = mediaMetadataDao.getMetadata(mediaStoreId)
        if (existing == null) {
            mediaMetadataDao.upsertMetadata(
                MediaMetadataEntity(
                    mediaStoreId = mediaStoreId,
                    mediaUriString = uriString,
                    isTrash = true,
                    trashTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            mediaMetadataDao.moveToTrash(mediaStoreId)
        }
    }
}
