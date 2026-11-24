package com.gallery.privatevault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.model.VirtualFolder
import com.gallery.privatevault.core.theme.GalleryTheme

@Composable
fun PinnedFoldersRow(
    folders: List<VirtualFolder>,
    onFolderClick: (VirtualFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = GalleryTheme.colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Carpetas Fijadas",
                color = GalleryTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(folders, key = { it.id }) { folder ->
                PinnedFolderChip(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}

@Composable
fun PinnedFolderChip(
    folder: VirtualFolder,
    onClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GalleryTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FolderSpecial,
            contentDescription = folder.name,
            tint = GalleryTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = folder.name,
            color = GalleryTheme.colors.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
