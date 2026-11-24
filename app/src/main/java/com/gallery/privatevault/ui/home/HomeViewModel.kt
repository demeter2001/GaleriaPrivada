package com.gallery.privatevault.ui.home

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.VirtualFolder
import com.gallery.privatevault.core.security.BiometricAuthManager
import com.gallery.privatevault.data.repository.MediaRepository
import com.gallery.privatevault.data.repository.ProfileRepository
import com.gallery.privatevault.data.repository.VirtualFolderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeProfile: ProfileEntity? = null,
    val availableProfiles: List<ProfileEntity> = emptyList(),
    val pinnedFolders: List<VirtualFolder> = emptyList(),
    val recentImages: List<MediaItem> = emptyList(),
    val recentVideos: List<MediaItem> = emptyList(),
    val recentAllMedia: List<MediaItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<MediaItem> = emptyList(),
    val isSearchActive: Boolean = false,
    val isProfileSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val hasStoragePermission: Boolean = true,
    val securityNoticeMessage: String? = null
)

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val mediaRepository: MediaRepository,
    private val virtualFolderRepository: VirtualFolderRepository,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _isProfileSheetVisible = MutableStateFlow(false)
    private val _securityNotice = MutableStateFlow<String?>(null)
    private val _hasStoragePermission = MutableStateFlow(true)

    private val _searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else mediaRepository.searchPermittedMedia(query)
        }

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.activeProfileWithDetails,
        profileRepository.allProfiles,
        virtualFolderRepository.pinnedFolders,
        mediaRepository.getRecentImages(limit = 12),
        mediaRepository.getRecentVideos(limit = 12),
        mediaRepository.permittedMediaFlow,
        _searchQuery,
        _searchResults,
        _isSearchActive,
        _isProfileSheetVisible,
        _hasStoragePermission,
        _securityNotice
    ) { params ->
        val activeProfileWithDetails = params[0] as? com.gallery.privatevault.core.database.relation.ProfileWithDetails
        @Suppress("UNCHECKED_CAST")
        val availableProfiles = params[1] as List<ProfileEntity>
        @Suppress("UNCHECKED_CAST")
        val pinnedFolders = params[2] as List<VirtualFolder>
        @Suppress("UNCHECKED_CAST")
        val recentImages = params[3] as List<MediaItem>
        @Suppress("UNCHECKED_CAST")
        val recentVideos = params[4] as List<MediaItem>
        @Suppress("UNCHECKED_CAST")
        val allPermitted = params[5] as List<MediaItem>
        val searchQuery = params[6] as String
        @Suppress("UNCHECKED_CAST")
        val searchResults = params[7] as List<MediaItem>
        val isSearchActive = params[8] as Boolean
        val isProfileSheetVisible = params[9] as Boolean
        val hasPermission = params[10] as Boolean
        val securityNotice = params[11] as? String

        HomeUiState(
            activeProfile = activeProfileWithDetails?.profile,
            availableProfiles = availableProfiles,
            pinnedFolders = pinnedFolders,
            recentImages = recentImages,
            recentVideos = recentVideos,
            recentAllMedia = allPermitted.take(30),
            searchQuery = searchQuery,
            searchResults = searchResults,
            isSearchActive = isSearchActive,
            isProfileSheetVisible = isProfileSheetVisible,
            isLoading = false,
            hasStoragePermission = hasPermission,
            securityNoticeMessage = securityNotice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _isSearchActive.value = query.isNotBlank()
    }

    fun setProfileSheetVisible(visible: Boolean) {
        _isProfileSheetVisible.value = visible
    }

    fun onPermissionStatusChanged(granted: Boolean) {
        _hasStoragePermission.value = granted
    }

    /**
     * Cambio seguro de perfil:
     * Si el perfil destino está protegido con PIN o Biometría, se invoca
     * el diálogo de BiometricPrompt antes de otorgar el cambio.
     */
    fun selectProfile(profile: ProfileEntity, activity: FragmentActivity) {
        if (!profile.isProtected) {
            viewModelScope.launch {
                profileRepository.setActiveProfile(profile.id)
            }
            return
        }

        // Perfil protegido: requerir autenticación
        viewModelScope.launch {
            val authenticated = biometricAuthManager.authenticate(
                activity = activity,
                title = "Perfil Protegido: ${profile.name}",
                subtitle = "Confirma tu identidad para cambiar a este perfil"
            )

            if (authenticated) {
                profileRepository.setActiveProfile(profile.id)
                _securityNotice.value = "Perfil '${profile.name}' desbloqueado"
            } else {
                _securityNotice.value = "Autenticación fallida. Acceso denegado."
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(item.id, item.uri.toString(), item.isFavorite)
        }
    }

    fun clearSecurityNotice() {
        _securityNotice.value = null
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val mediaRepository: MediaRepository,
        private val virtualFolderRepository: VirtualFolderRepository,
        private val biometricAuthManager: BiometricAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                profileRepository,
                mediaRepository,
                virtualFolderRepository,
                biometricAuthManager
            ) as T
        }
    }
}
