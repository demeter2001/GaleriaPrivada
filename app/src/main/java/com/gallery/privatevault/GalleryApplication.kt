package com.gallery.privatevault

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.gallery.privatevault.core.database.GalleryDatabase
import com.gallery.privatevault.core.mediastore.MediaStoreDataSource
import com.gallery.privatevault.core.security.BiometricAuthManager
import com.gallery.privatevault.data.repository.MediaRepository
import com.gallery.privatevault.data.repository.ProfileRepository
import com.gallery.privatevault.data.repository.ThemeRepository
import com.gallery.privatevault.data.repository.VirtualFolderRepository

class GalleryApplication : Application(), ImageLoaderFactory {

    val database by lazy { GalleryDatabase.getInstance(this) }

    val mediaStoreDataSource by lazy {
        MediaStoreDataSource(this, database.mediaMetadataDao())
    }

    val profileRepository by lazy {
        ProfileRepository(database.profileDao(), database.appSettingsDao())
    }

    val mediaRepository by lazy {
        MediaRepository(mediaStoreDataSource, profileRepository, database.mediaMetadataDao())
    }

    val virtualFolderRepository by lazy {
        VirtualFolderRepository(database.virtualFolderDao(), profileRepository)
    }

    val themeRepository by lazy {
        ThemeRepository(database.themeDao(), database.appSettingsDao(), profileRepository)
    }

    val biometricAuthManager by lazy {
        BiometricAuthManager(this)
    }

    /**
     * Configuración de Coil optimizada para rendimiento fluido:
     * - Decodificador nativo de fotogramas de video
     * - Soporte para GIFs animados y SVGs
     * - Caché en memoria agresiva (25% del espacio disponible)
     * - Caché en disco para miniaturas
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_thumbnails_cache"))
                    .maxSizeBytes(150 * 1024 * 1024) // 150 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
