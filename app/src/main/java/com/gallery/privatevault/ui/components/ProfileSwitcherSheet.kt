package com.gallery.privatevault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.database.entity.ProfileEntity
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    activeProfileId: Long,
    profiles: List<ProfileEntity>,
    onSelectProfile: (ProfileEntity) -> Unit,
    onCreateProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GalleryTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GalleryTheme.colors.onSurface.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seleccionar Perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GalleryTheme.colors.onSurface
                )

                TextButton(onClick = onCreateProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuevo Perfil",
                        tint = GalleryTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Crear", color = GalleryTheme.colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == activeProfileId
                    ProfileItemRow(
                        profile = profile,
                        isSelected = isSelected,
                        onClick = {
                            onSelectProfile(profile)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileItemRow(
    profile: ProfileEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (isSelected) GalleryTheme.colors.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (profile.isProtected) Icons.Default.Lock else Icons.Outlined.Folder,
            contentDescription = null,
            tint = if (isSelected) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    color = if (isSelected) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (profile.isReadOnly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Solo Lectura",
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(GalleryTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = if (profile.includeAllByDefault) "Todo el dispositivo" else "Carpetas seleccionadas",
                color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Activo",
                tint = GalleryTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
