package com.gallery.privatevault.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.database.dao.AppSettingsDao
import com.gallery.privatevault.core.database.dao.MediaMetadataDao
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrashItem(
    val mediaItem: MediaItem,
    val daysRemaining: Int
)

data class TrashUiState(
    val items: List<TrashItem> = emptyList(),
    val retentionDays: Int = 30,
    val isLoading: Boolean = true
)

class TrashViewModel(
    private val mediaMetadataDao: MediaMetadataDao,
    private val appSettingsDao: AppSettingsDao,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    init {
        // Purgar automáticamente elementos cuyo tiempo de retención haya expirado
        viewModelScope.launch {
            val settings = appSettingsDao.getSettings()
            val retentionDays = settings?.trashRetentionDays ?: 30
            val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
            mediaMetadataDao.purgeExpiredTrash(cutoff)
        }
    }

    val uiState: StateFlow<TrashUiState> = combine(
        mediaMetadataDao.getTrashMediaFlow(),
        appSettingsDao.getSettingsFlow()
    ) { trashEntities, settings ->
        val retentionDays = settings?.trashRetentionDays ?: 30
        val retentionMs = retentionDays.toLong() * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        // Mapear entidades de papelera a MediaItems
        val trashItems = trashEntities.map { entity ->
            val elapsed = now - (entity.trashTimestamp ?: now)
            val remainingMs = (retentionMs - elapsed).coerceAtLeast(0L)
            val days = (remainingMs / (24 * 60 * 60 * 1000)).toInt()

            val dummyMedia = MediaItem(
                id = entity.mediaStoreId,
                uri = android.net.Uri.parse(entity.mediaUriString),
                path = entity.mediaUriString,
                displayName = entity.mediaUriString.substringAfterLast('/'),
                mimeType = "image/jpeg",
                sizeBytes = 0L,
                dateAddedSec = 0L,
                dateModifiedSec = 0L,
                width = 0,
                height = 0,
                isTrash = true
            )

            TrashItem(mediaItem = dummyMedia, daysRemaining = days)
        }

        TrashUiState(
            items = trashItems,
            retentionDays = retentionDays,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState()
    )

    fun restoreItem(mediaStoreId: Long) {
        viewModelScope.launch {
            mediaMetadataDao.restoreFromTrash(mediaStoreId)
        }
    }

    fun purgeAll() {
        viewModelScope.launch {
            mediaMetadataDao.purgeExpiredTrash(System.currentTimeMillis() + 1000L) // Purgar todo
        }
    }

    class Factory(
        private val mediaMetadataDao: MediaMetadataDao,
        private val appSettingsDao: AppSettingsDao,
        private val mediaRepository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrashViewModel(mediaMetadataDao, appSettingsDao, mediaRepository) as T
        }
    }
}
