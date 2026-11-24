package com.gallery.privatevault.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class VideoResolutionFilter(val label: String) {
    ALL("Todos"),
    FOUR_K("4K Ultra HD"),
    FULL_HD("Full HD (1080p)"),
    HD("HD (720p)"),
    SHORTS("Cortos (< 1 min)")
}

data class VideosUiState(
    val videos: List<MediaItem> = emptyList(),
    val filteredVideos: List<MediaItem> = emptyList(),
    val selectedFilter: VideoResolutionFilter = VideoResolutionFilter.ALL,
    val onlyFavorites: Boolean = false,
    val columnCount: Int = 2,
    val isLoading: Boolean = true
)

class VideosViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(VideoResolutionFilter.ALL)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _columnCount = MutableStateFlow(2)

    val uiState: StateFlow<VideosUiState> = combine(
        mediaRepository.permittedMediaFlow,
        _selectedFilter,
        _onlyFavorites,
        _columnCount
    ) { allMedia, filter, onlyFav, columns ->
        val rawVideos = allMedia.filter { it.isVideo }

        val filtered = rawVideos.filter { item ->
            val matchesFavorite = if (onlyFav) item.isFavorite else true
            val maxDim = maxOf(item.width, item.height)
            val matchesRes = when (filter) {
                VideoResolutionFilter.ALL -> true
                VideoResolutionFilter.FOUR_K -> maxDim >= 3800
                VideoResolutionFilter.FULL_HD -> maxDim in 1900..3799
                VideoResolutionFilter.HD -> maxDim in 1200..1899
                VideoResolutionFilter.SHORTS -> item.durationMs in 1..60_000
            }
            matchesFavorite && matchesRes
        }

        VideosUiState(
            videos = rawVideos,
            filteredVideos = filtered,
            selectedFilter = filter,
            onlyFavorites = onlyFav,
            columnCount = columns,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VideosUiState()
    )

    fun onFilterSelected(filter: VideoResolutionFilter) {
        _selectedFilter.value = filter
    }

    fun onToggleFavoritesOnly() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun onToggleColumnCount() {
        val current = _columnCount.value
        _columnCount.value = if (current >= 3) 1 else current + 1
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
            return VideosViewModel(mediaRepository) as T
        }
    }
}
