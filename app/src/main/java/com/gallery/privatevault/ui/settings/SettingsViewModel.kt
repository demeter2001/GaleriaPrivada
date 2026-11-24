package com.gallery.privatevault.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.database.dao.AppSettingsDao
import com.gallery.privatevault.core.database.entity.AppSettingsEntity
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.database.relation.ProfileWithDetails
import com.gallery.privatevault.core.model.NavigationStyle
import com.gallery.privatevault.core.model.RuleType
import com.gallery.privatevault.core.model.ThemeConfig
import com.gallery.privatevault.core.security.SecurityUtils
import com.gallery.privatevault.data.repository.ProfileRepository
import com.gallery.privatevault.data.repository.ThemeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profiles: List<ProfileWithDetails> = emptyList(),
    val settings: AppSettingsEntity? = null,
    val availableThemes: List<ThemeConfig> = emptyList(),
    val selectedProfileForEdit: ProfileWithDetails? = null,
    val isEditProfileDialogVisible: Boolean = false,
    val isAddRuleDialogVisible: Boolean = false
)

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val appSettingsDao: AppSettingsDao,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _selectedProfileForEdit = MutableStateFlow<ProfileWithDetails?>(null)
    private val _isEditProfileDialogVisible = MutableStateFlow(false)
    private val _isAddRuleDialogVisible = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        profileRepository.allProfilesWithDetails,
        appSettingsDao.getSettingsFlow(),
        themeRepository.allThemes,
        _selectedProfileForEdit,
        _isEditProfileDialogVisible,
        _isAddRuleDialogVisible
    ) { profiles, settings, themes, selectedProfile, editDialog, ruleDialog ->
        SettingsUiState(
            profiles = profiles,
            settings = settings,
            availableThemes = themes,
            selectedProfileForEdit = selectedProfile,
            isEditProfileDialogVisible = editDialog,
            isAddRuleDialogVisible = ruleDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onSelectProfileToEdit(profile: ProfileWithDetails) {
        _selectedProfileForEdit.value = profile
        _isEditProfileDialogVisible.value = true
    }

    fun closeEditProfileDialog() {
        _isEditProfileDialogVisible.value = false
        _selectedProfileForEdit.value = null
    }

    fun showAddRuleDialog(visible: Boolean) {
        _isAddRuleDialogVisible.value = visible
    }

    fun saveProfile(
        id: Long,
        name: String,
        isProtected: Boolean,
        pin: String?,
        isReadOnly: Boolean,
        linkedThemeId: String?,
        includeAllByDefault: Boolean
    ) {
        viewModelScope.launch {
            val pinHash = if (isProtected && !pin.isNullOrBlank()) SecurityUtils.hashPin(pin) else null

            if (id == 0L) {
                // Crear nuevo perfil
                profileRepository.createProfile(
                    name = name,
                    isProtected = isProtected,
                    pinHash = pinHash,
                    isReadOnly = isReadOnly,
                    linkedThemeId = linkedThemeId,
                    includeAllByDefault = includeAllByDefault
                )
            } else {
                // Actualizar perfil existente
                _selectedProfileForEdit.value?.profile?.let { existing ->
                    profileRepository.updateProfile(
                        existing.copy(
                            name = name,
                            isProtected = isProtected,
                            pinHash = pinHash ?: existing.pinHash,
                            isReadOnly = isReadOnly,
                            linkedThemeId = linkedThemeId,
                            includeAllByDefault = includeAllByDefault
                        )
                    )
                }
            }
            closeEditProfileDialog()
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (!profile.isDefault) {
                profileRepository.deleteProfile(profile)
                closeEditProfileDialog()
            }
        }
    }

    fun addFolderRule(profileId: Long, folderPath: String, ruleType: RuleType) {
        viewModelScope.launch {
            if (folderPath.isNotBlank()) {
                profileRepository.addFolderRule(profileId, folderPath.trim(), ruleType)
                _isAddRuleDialogVisible.value = false
            }
        }
    }

    fun removeFolderRule(ruleId: Long) {
        viewModelScope.launch {
            profileRepository.removeRule(ruleId)
        }
    }

    fun updateNavigationStyle(style: NavigationStyle) {
        viewModelScope.launch {
            appSettingsDao.updateNavigationStyle(style)
        }
    }

    fun updateTrashRetention(days: Int) {
        viewModelScope.launch {
            appSettingsDao.updateTrashRetentionDays(days)
        }
    }

    fun updateAutoMaxBrightness(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDao.updateAutoMaxBrightness(enabled)
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val appSettingsDao: AppSettingsDao,
        private val themeRepository: ThemeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(profileRepository, appSettingsDao, themeRepository) as T
        }
    }
}
