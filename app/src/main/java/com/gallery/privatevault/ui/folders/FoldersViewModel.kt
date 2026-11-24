package com.gallery.privatevault.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.VirtualFolder
import com.gallery.privatevault.data.repository.MediaRepository
import com.gallery.privatevault.data.repository.VirtualFolderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RealFolder(
    val bucketId: String,
    val name: String,
    val coverItem: MediaItem?,
    val itemCount: Int,
    val items: List<MediaItem>
)

enum class FolderTabFilter {
    ALL,
    REAL,
    VIRTUAL
}

data class FoldersUiState(
    val realFolders: List<RealFolder> = emptyList(),
    val virtualFolders: List<VirtualFolder> = emptyList(),
    val selectedTab: FolderTabFilter = FolderTabFilter.ALL,
    val activeOpenedFolder: RealFolder? = null,
    val activeOpenedVirtualFolder: VirtualFolder? = null,
    val activeVirtualFolderItems: List<MediaItem> = emptyList(),
    val isCreateVirtualFolderDialogVisible: Boolean = false,
    val isLoading: Boolean = true
)

class FoldersViewModel(
    private val mediaRepository: MediaRepository,
    private val virtualFolderRepository: VirtualFolderRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FolderTabFilter.ALL)
    private val _activeRealFolder = MutableStateFlow<RealFolder?>(null)
    private val _activeVirtualFolder = MutableStateFlow<VirtualFolder?>(null)
    private val _isCreateDialogVisible = MutableStateFlow(false)

    val uiState: StateFlow<FoldersUiState> = combine(
        mediaRepository.permittedMediaFlow,
        virtualFolderRepository.activeProfileVirtualFolders,
        _selectedTab,
        _activeRealFolder,
        _activeVirtualFolder,
        _isCreateDialogVisible
    ) { allMedia, virtualFolders, tab, activeReal, activeVirtual, isDialogVisible ->
        // Agrupar carpetas reales del almacenamiento que pertenezcan al perfil activo
        val realFoldersMap = allMedia
            .groupBy { it.bucketDisplayName.ifBlank { "Almacenamiento Interno" } }
            .map { (bucketName, items) ->
                RealFolder(
                    bucketId = items.firstOrNull()?.bucketId ?: bucketName,
                    name = bucketName,
                    coverItem = items.firstOrNull(),
                    itemCount = items.size,
                    items = items
                )
            }
            .sortedByDescending { it.itemCount }

        // Si hay una carpeta real abierta, actualizar sus items
        val updatedActiveReal = activeReal?.let { current ->
            realFoldersMap.find { it.name == current.name }
        }

        FoldersUiState(
            realFolders = realFoldersMap,
            virtualFolders = virtualFolders,
            selectedTab = tab,
            activeOpenedFolder = updatedActiveReal,
            activeOpenedVirtualFolder = activeVirtual,
            isCreateVirtualFolderDialogVisible = isDialogVisible,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FoldersUiState()
    )

    fun onSelectTab(tab: FolderTabFilter) {
        _selectedTab.value = tab
    }

    fun openRealFolder(folder: RealFolder) {
        _activeRealFolder.value = folder
    }

    fun openVirtualFolder(folder: VirtualFolder) {
        _activeVirtualFolder.value = folder
    }

    fun closeActiveFolder() {
        _activeRealFolder.value = null
        _activeVirtualFolder.value = null
    }

    fun showCreateVirtualFolderDialog(visible: Boolean) {
        _isCreateDialogVisible.value = visible
    }

    fun createVirtualFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                virtualFolderRepository.createVirtualFolder(name.trim())
                _isCreateDialogVisible.value = false
            }
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val virtualFolderRepository: VirtualFolderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FoldersViewModel(mediaRepository, virtualFolderRepository) as T
        }
    }
}
