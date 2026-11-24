package com.gallery.privatevault.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gallery.privatevault.core.model.ThemeConfig
import com.gallery.privatevault.data.repository.ThemeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ThemesUiState(
    val currentTheme: ThemeConfig = ThemeRepository.defaultThemeConfig,
    val availableThemes: List<ThemeConfig> = emptyList(),
    val editingTheme: ThemeConfig? = null,
    val isEditingTheme: Boolean = false,
    val jsonExportData: String? = null,
    val importError: String? = null
)

class ThemesViewModel(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _editingTheme = MutableStateFlow<ThemeConfig?>(null)
    private val _isEditing = MutableStateFlow(false)
    private val _jsonExport = MutableStateFlow<String?>(null)
    private val _importError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ThemesUiState> = combine(
        themeRepository.activeThemeConfig,
        themeRepository.allThemes,
        _editingTheme,
        _isEditing,
        _jsonExport,
        _importError
    ) { current, all, editing, isEdit, jsonExport, importErr ->
        ThemesUiState(
            currentTheme = current,
            availableThemes = all,
            editingTheme = editing,
            isEditingTheme = isEdit,
            jsonExportData = jsonExport,
            importError = importErr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemesUiState()
    )

    fun onSelectTheme(theme: ThemeConfig) {
        viewModelScope.launch {
            themeRepository.setActiveTheme(theme.id)
        }
    }

    fun startCustomizingTheme(theme: ThemeConfig) {
        _editingTheme.value = theme.copy(
            id = "custom_${System.currentTimeMillis()}",
            name = "${theme.name} (Copia)",
            isBuiltIn = false
        )
        _isEditing.value = true
    }

    fun updateEditingTheme(updated: ThemeConfig) {
        _editingTheme.value = updated
    }

    fun saveEditingTheme() {
        viewModelScope.launch {
            _editingTheme.value?.let { theme ->
                themeRepository.saveTheme(theme)
                themeRepository.setActiveTheme(theme.id)
            }
            _isEditing.value = false
            _editingTheme.value = null
        }
    }

    fun cancelEditing() {
        _isEditing.value = false
        _editingTheme.value = null
    }

    fun exportThemeToJson(theme: ThemeConfig) {
        _jsonExport.value = themeRepository.exportThemeToJson(theme)
    }

    fun clearExportData() {
        _jsonExport.value = null
    }

    fun importThemeFromJson(jsonText: String) {
        val result = themeRepository.importThemeFromJson(jsonText)
        result.onSuccess { imported ->
            viewModelScope.launch {
                themeRepository.saveTheme(imported)
                themeRepository.setActiveTheme(imported.id)
                _importError.value = null
            }
        }.onFailure {
            _importError.value = "JSON inválido o corrupto"
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    class Factory(
        private val themeRepository: ThemeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemesViewModel(themeRepository) as T
        }
    }
}
