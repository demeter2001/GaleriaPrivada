package com.gallery.privatevault.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ImageFormatFilter(val label: String) {
    ALL("Todas"),
    JPG_PNG("JPG/PNG"),
    WEBP("WEBP"),
    GIF("GIF Animado"),
    RAW_DNG("RAW/DNG"),
    SVG("Vector SVG"),
    HEIC("HEIC/HEIF")
}

data class ImagesUiState(
    val images: List<MediaItem> = emptyList(),
    val filteredImages: List<MediaItem> = emptyList(),
    val selectedFormat: ImageFormatFilter = ImageFormatFilter.ALL,
    val onlyFavorites: Boolean = false,
    val columnCount: Int = 3,
    val isLoading: Boolean = true
)

class ImagesViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _selectedFormat = MutableStateFlow(ImageFormatFilter.ALL)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _columnCount = MutableStateFlow(3)

    val uiState: StateFlow<ImagesUiState> = combine(
        mediaRepository.permittedMediaFlow,
        _selectedFormat,
        _onlyFavorites,
        _columnCount
    ) { allMedia, formatFilter, onlyFav, columns ->
        val rawImages = allMedia.filter { !it.isVideo }

        val filtered = rawImages.filter { item ->
            val matchesFavorite = if (onlyFav) item.isFavorite else true
            val matchesFormat = when (formatFilter) {
                ImageFormatFilter.ALL -> true
                ImageFormatFilter.JPG_PNG -> item.mimeType == "image/jpeg" || item.mimeType == "image/png"
                ImageFormatFilter.WEBP -> item.mimeType == "image/webp"
                ImageFormatFilter.GIF -> item.mimeType == "image/gif"
                ImageFormatFilter.RAW_DNG -> item.mimeType.contains("dng") || item.mimeType.contains("raw") || item.displayName.endsWith(".dng", true)
                ImageFormatFilter.SVG -> item.mimeType == "image/svg+xml" || item.displayName.endsWith(".svg", true)
                ImageFormatFilter.HEIC -> item.mimeType == "image/heif" || item.mimeType == "image/heic"
            }
            matchesFavorite && matchesFormat
        }

        ImagesUiState(
            images = rawImages,
            filteredImages = filtered,
            selectedFormat = formatFilter,
            onlyFavorites = onlyFav,
            columnCount = columns,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ImagesUiState()
    )

    fun onFormatFilterSelected(filter: ImageFormatFilter) {
        _selectedFormat.value = filter
    }

    fun onToggleFavoritesOnly() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun onToggleColumnCount() {
        val current = _columnCount.value
        _columnCount.value = if (current >= 4) 2 else current + 1
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(item.id, item.uri.toString(), item.isFavorite)
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImagesViewModel(mediaRepository) as T
        }
    }
}
