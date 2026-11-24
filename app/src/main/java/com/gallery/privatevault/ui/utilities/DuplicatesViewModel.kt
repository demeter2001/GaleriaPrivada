package com.gallery.privatevault.ui.utilities

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.duplicates.DuplicateFinder
import com.gallery.privatevault.core.duplicates.DuplicateGroup
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DuplicatesUiState(
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isScanning: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val selectedItemIds: Set<Long> = emptySet(),
    val totalWastedBytes: Long = 0L,
    val isCompleted: Boolean = false
)

class DuplicatesViewModel(
    private val context: Context,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val finder = DuplicateFinder(context)
    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, isCompleted = false, selectedItemIds = emptySet()) }

            val permittedMedia = mediaRepository.permittedMediaFlow.first()
            val results = finder.findDuplicates(permittedMedia) { current, total ->
                _uiState.update { it.copy(progressCurrent = current, progressTotal = total) }
            }

            val wasted = results.sumOf { it.wastedBytes }

            // Preseleccionar por defecto todas las copias excepto la primera (original más antiguo)
            val autoSelected = results.flatMap { group ->
                group.items.drop(1).map { it.id }
            }.toSet()

            _uiState.update {
                it.copy(
                    duplicateGroups = results,
                    isScanning = false,
                    isCompleted = true,
                    totalWastedBytes = wasted,
                    selectedItemIds = autoSelected
                )
            }
        }
    }

    fun toggleItemSelection(itemId: Long) {
        _uiState.update { state ->
            val set = state.selectedItemIds.toMutableSet()
            if (set.contains(itemId)) set.remove(itemId) else set.add(itemId)
            state.copy(selectedItemIds = set)
        }
    }

    fun moveSelectedToTrash() {
        viewModelScope.launch {
            val toTrash = _uiState.value.selectedItemIds
            val allItems = _uiState.value.duplicateGroups.flatMap { it.items }
            allItems.filter { toTrash.contains(it.id) }.forEach { item ->
                mediaRepository.moveToTrash(item.id, item.uri.toString())
            }
            // Re-escanear o limpiar de la lista
            _uiState.update {
                it.copy(
                    duplicateGroups = emptyList(),
                    selectedItemIds = emptySet(),
                    isCompleted = false
                )
            }
        }
    }

    class Factory(
        private val context: Context,
        private val mediaRepository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DuplicatesViewModel(context, mediaRepository) as T
        }
    }
}
