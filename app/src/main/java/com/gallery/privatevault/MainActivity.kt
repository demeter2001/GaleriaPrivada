package com.gallery.privatevault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gallery.privatevault.core.model.MediaItem
import com.gallery.privatevault.core.model.NavigationStyle
import com.gallery.privatevault.core.theme.PrivateGalleryTheme
import com.gallery.privatevault.ui.editor.ImageEditorScreen
import com.gallery.privatevault.ui.folders.FoldersScreen
import com.gallery.privatevault.ui.folders.FoldersViewModel
import com.gallery.privatevault.ui.home.HomeScreen
import com.gallery.privatevault.ui.home.HomeViewModel
import com.gallery.privatevault.ui.images.ImagesScreen
import com.gallery.privatevault.ui.images.ImagesViewModel
import com.gallery.privatevault.ui.navigation.GalleryDestination
import com.gallery.privatevault.ui.navigation.GalleryNavigationShell
import com.gallery.privatevault.ui.settings.SettingsScreen
import com.gallery.privatevault.ui.settings.SettingsViewModel
import com.gallery.privatevault.ui.themes.ThemesScreen
import com.gallery.privatevault.ui.themes.ThemesViewModel
import com.gallery.privatevault.ui.utilities.DuplicatesScreen
import com.gallery.privatevault.ui.utilities.DuplicatesViewModel
import com.gallery.privatevault.ui.utilities.TrashScreen
import com.gallery.privatevault.ui.utilities.TrashViewModel
import com.gallery.privatevault.ui.videos.VideosScreen
import com.gallery.privatevault.ui.videos.VideosViewModel
import com.gallery.privatevault.ui.viewer.MediaViewerScreen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as GalleryApplication

        setContent {
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            val themeConfig by app.themeRepository.activeThemeConfig.collectAsState(
                initial = com.gallery.privatevault.data.repository.ThemeRepository.defaultThemeConfig
            )

            var currentDestination by remember { mutableStateOf(GalleryDestination.HOME) }
            var activeSubView by remember { mutableStateOf<String?>(null) } // "TRASH" o "DUPLICATES"
            var editingItem by remember { mutableStateOf<MediaItem?>(null) }

            // ViewModels principales
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    profileRepository = app.profileRepository,
                    mediaRepository = app.mediaRepository,
                    virtualFolderRepository = app.virtualFolderRepository,
                    biometricAuthManager = app.biometricAuthManager
                )
            )

            val imagesViewModel: ImagesViewModel = viewModel(
                factory = ImagesViewModel.Factory(app.mediaRepository)
            )

            val videosViewModel: VideosViewModel = viewModel(
                factory = VideosViewModel.Factory(app.mediaRepository)
            )

            val foldersViewModel: FoldersViewModel = viewModel(
                factory = FoldersViewModel.Factory(app.mediaRepository, app.virtualFolderRepository)
            )

            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.profileRepository, app.database.appSettingsDao(), app.themeRepository)
            )

            val themesViewModel: ThemesViewModel = viewModel(
                factory = ThemesViewModel.Factory(app.themeRepository)
            )

            val duplicatesViewModel: DuplicatesViewModel = viewModel(
                factory = DuplicatesViewModel.Factory(this@MainActivity, app.mediaRepository)
            )

            val trashViewModel: TrashViewModel = viewModel(
                factory = TrashViewModel.Factory(app.database.mediaMetadataDao(), app.database.appSettingsDao(), app.mediaRepository)
            )

            // Permisos de lectura multimedia
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            var hasPermission by remember {
                mutableStateOf(
                    permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                hasPermission = allGranted
                homeViewModel.onPermissionStatusChanged(allGranted)
            }

            LaunchedEffect(Unit) {
                if (!hasPermission) {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }

            // Estado del Visor Multimedia Activo
            var viewerItem by remember { mutableStateOf<MediaItem?>(null) }
            var viewerContextList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

            PrivateGalleryTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (editingItem != null) {
                        BackHandler {
                            editingItem = null
                        }
                        ImageEditorScreen(
                            item = editingItem!!,
                            onClose = { editingItem = null },
                            onSavedSuccessfully = { editingItem = null }
                        )
                    } else if (viewerItem != null) {
                        BackHandler {
                            viewerItem = null
                        }
                        MediaViewerScreen(
                            initialItem = viewerItem!!,
                            contextList = viewerContextList,
                            onClose = { viewerItem = null },
                            onToggleFavorite = { item ->
                                homeViewModel.toggleFavorite(item)
                            },
                            onDeleteMedia = { item ->
                                lifecycleScope.launch {
                                    app.mediaRepository.moveToTrash(item.id, item.uri.toString())
                                }
                            },
                            onEditImage = { item ->
                                editingItem = item
                            }
                        )
                    } else if (activeSubView != null) {
                        BackHandler {
                            activeSubView = null
                        }
                        when (activeSubView) {
                            "TRASH" -> TrashScreen(
                                viewModel = trashViewModel,
                                onBack = { activeSubView = null }
                            )
                            "DUPLICATES" -> DuplicatesScreen(
                                viewModel = duplicatesViewModel,
                                onBack = { activeSubView = null }
                            )
                        }
                    } else {
                        // Retroceder a Inicio antes de salir de la app si se está en otra pestaña
                        BackHandler(enabled = currentDestination != GalleryDestination.HOME) {
                            currentDestination = GalleryDestination.HOME
                        }

                        GalleryNavigationShell(
                            currentDestination = currentDestination,
                            onDestinationSelected = { destination ->
                                currentDestination = destination
                                scope.launch { drawerState.close() }
                            },
                            navigationStyle = NavigationStyle.HYBRID,
                            drawerState = drawerState,
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (currentDestination) {
                                    GalleryDestination.HOME -> {
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            onNavigateToMediaViewer = { item, contextList ->
                                                viewerItem = item
                                                viewerContextList = contextList
                                            },
                                            onNavigateToImages = { currentDestination = GalleryDestination.IMAGES },
                                            onNavigateToVideos = { currentDestination = GalleryDestination.VIDEOS },
                                            onNavigateToFolders = { currentDestination = GalleryDestination.FOLDERS },
                                            onNavigateToSettings = { currentDestination = GalleryDestination.SETTINGS }
                                        )
                                    }
                                    GalleryDestination.IMAGES -> {
                                        ImagesScreen(
                                            viewModel = imagesViewModel,
                                            onNavigateToMediaViewer = { item, contextList ->
                                                viewerItem = item
                                                viewerContextList = contextList
                                            },
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    GalleryDestination.VIDEOS -> {
                                        VideosScreen(
                                            viewModel = videosViewModel,
                                            onNavigateToMediaViewer = { item, contextList ->
                                                viewerItem = item
                                                viewerContextList = contextList
                                            },
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    GalleryDestination.FOLDERS -> {
                                        FoldersScreen(
                                            viewModel = foldersViewModel,
                                            onNavigateToMediaViewer = { item, contextList ->
                                                viewerItem = item
                                                viewerContextList = contextList
                                            },
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    GalleryDestination.THEMES -> {
                                        ThemesScreen(
                                            viewModel = themesViewModel,
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                    GalleryDestination.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            onNavigateToTrash = { activeSubView = "TRASH" },
                                            onNavigateToDuplicates = { activeSubView = "DUPLICATES" },
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
