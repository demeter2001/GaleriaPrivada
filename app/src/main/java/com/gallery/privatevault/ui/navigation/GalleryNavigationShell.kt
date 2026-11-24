package com.gallery.privatevault.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.model.NavigationStyle
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryNavigationShell(
    currentDestination: GalleryDestination,
    onDestinationSelected: (GalleryDestination) -> Unit,
    navigationStyle: NavigationStyle,
    drawerState: DrawerState,
    onOpenDrawer: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val showBottomBar = navigationStyle == NavigationStyle.BOTTOM_BAR || navigationStyle == NavigationStyle.HYBRID
    val showDrawer = navigationStyle == NavigationStyle.DRAWER || navigationStyle == NavigationStyle.HYBRID

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = GalleryTheme.colors.surface,
                    drawerContentColor = GalleryTheme.colors.onSurface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Galería Privada",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GalleryTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    Text(
                        text = "Privacidad Extrema & Cero Telemetría",
                        fontSize = 12.sp,
                        color = GalleryTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GalleryDestination.values().forEach { destination ->
                        val selected = destination == currentDestination
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title,
                                    tint = if (selected) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) GalleryTheme.colors.primary else GalleryTheme.colors.onSurface
                                )
                            },
                            selected = selected,
                            onClick = {
                                onDestinationSelected(destination)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = GalleryTheme.colors.primary.copy(alpha = 0.15f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        GalleryBottomNavigationBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = onDestinationSelected
                        )
                    }
                },
                containerColor = GalleryTheme.colors.background,
                content = content
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    GalleryBottomNavigationBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = onDestinationSelected
                    )
                }
            },
            containerColor = GalleryTheme.colors.background,
            content = content
        )
    }
}

@Composable
fun GalleryBottomNavigationBar(
    currentDestination: GalleryDestination,
    onDestinationSelected: (GalleryDestination) -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    NavigationBar(
        containerColor = GalleryTheme.colors.barBackground,
        contentColor = GalleryTheme.colors.onSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        // En la barra inferior mostramos los 4 destinos más frecuentes
        val bottomItems = listOf(
            GalleryDestination.HOME,
            GalleryDestination.IMAGES,
            GalleryDestination.VIDEOS,
            GalleryDestination.FOLDERS
        )

        bottomItems.forEach { destination ->
            val isSelected = destination == currentDestination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GalleryTheme.colors.primary,
                    selectedTextColor = GalleryTheme.colors.primary,
                    indicatorColor = GalleryTheme.colors.primary.copy(alpha = 0.18f),
                    unselectedIconColor = GalleryTheme.colors.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}
