package com.gallery.privatevault.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.privatevault.core.database.relation.ProfileWithDetails
import com.gallery.privatevault.core.model.NavigationStyle
import com.gallery.privatevault.core.model.RuleType
import com.gallery.privatevault.core.theme.GalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToTrash: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GalleryTheme.colors.barBackground,
                    titleContentColor = GalleryTheme.colors.onSurface,
                    navigationIconContentColor = GalleryTheme.colors.onSurface
                )
            )
        },
        containerColor = GalleryTheme.colors.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // SECCIÓN: GESTIÓN DE PERFILES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Perfiles y Reglas de Aislamiento",
                        fontWeight = FontWeight.Bold,
                        color = GalleryTheme.colors.primary,
                        fontSize = 15.sp
                    )
                    TextButton(onClick = {
                        viewModel.onSelectProfileToEdit(
                            ProfileWithDetails(
                                profile = com.gallery.privatevault.core.database.entity.ProfileEntity(id = 0, name = "Nuevo Perfil"),
                                rules = emptyList(),
                                virtualFolders = emptyList()
                            )
                        )
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo Perfil")
                    }
                }
            }

            items(uiState.profiles, key = { it.profile.id }) { profileDetails ->
                ProfileSettingsCard(
                    details = profileDetails,
                    onEditClick = { viewModel.onSelectProfileToEdit(profileDetails) }
                )
            }

            // SECCIÓN: NAVEGACIÓN GENERAL
            item {
                Text(
                    text = "Estilo de Navegación",
                    fontWeight = FontWeight.Bold,
                    color = GalleryTheme.colors.primary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                NavigationStyleSelector(
                    currentStyle = uiState.settings?.navigationStyle ?: NavigationStyle.HYBRID,
                    onStyleSelected = viewModel::updateNavigationStyle
                )
            }

            // SECCIÓN: COMPORTAMIENTO DEL VISOR
            item {
                Text(
                    text = "Visor Multimedia",
                    fontWeight = FontWeight.Bold,
                    color = GalleryTheme.colors.primary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = GalleryTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Brillo máximo automático", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GalleryTheme.colors.onSurface)
                            Text("Eleva el brillo al abrir fotos/videos y lo restaura al salir", fontSize = 12.sp, color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = uiState.settings?.autoMaxBrightness ?: false,
                            onCheckedChange = viewModel::updateAutoMaxBrightness
                        )
                    }
                }
            }

            // SECCIÓN: UTILIDADES Y LIMPIEZA
            item {
                Text(
                    text = "Utilidades Locales",
                    fontWeight = FontWeight.Bold,
                    color = GalleryTheme.colors.primary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityRowItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Papelera de Reciclaje",
                        subtitle = "Retención de archivos antes del borrado definitivo",
                        onClick = onNavigateToTrash
                    )
                    UtilityRowItem(
                        icon = Icons.Default.FilterNone,
                        title = "Buscador de Duplicados",
                        subtitle = "Escanea por tamaño y hash SHA-256 localmente",
                        onClick = onNavigateToDuplicates
                    )
                }
            }
        }

        // Diálogo de Edición de Perfil
        if (uiState.isEditProfileDialogVisible && uiState.selectedProfileForEdit != null) {
            ProfileEditorDialog(
                details = uiState.selectedProfileForEdit!!,
                availableThemes = uiState.availableThemes,
                onSave = viewModel::saveProfile,
                onDelete = viewModel::deleteProfile,
                onAddRule = viewModel::addFolderRule,
                onRemoveRule = viewModel::removeFolderRule,
                onDismiss = viewModel::closeEditProfileDialog
            )
        }
    }
}

@Composable
fun ProfileSettingsCard(
    details: ProfileWithDetails,
    onEditClick: () -> Unit
) {
    val cornerRadius = GalleryTheme.current.cardCornerRadius / 1.5f

    Surface(
        color = GalleryTheme.colors.surface,
        shape = RoundedCornerShape(cornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (details.profile.isProtected) Icons.Default.Lock else Icons.Default.Folder,
                contentDescription = null,
                tint = GalleryTheme.colors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = details.profile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GalleryTheme.colors.onSurface
                    )
                    if (details.profile.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Por Defecto",
                            fontSize = 9.sp,
                            color = GalleryTheme.colors.primary,
                            modifier = Modifier
                                .background(GalleryTheme.colors.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${details.rules.size} reglas configuradas · ${if (details.profile.includeAllByDefault) "Todo por defecto" else "Carpetas estrictas"}",
                    fontSize = 12.sp,
                    color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Editar", tint = GalleryTheme.colors.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun NavigationStyleSelector(
    currentStyle: NavigationStyle,
    onStyleSelected: (NavigationStyle) -> Unit
) {
    Surface(
        color = GalleryTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            NavigationStyle.values().forEach { style ->
                val label = when (style) {
                    NavigationStyle.BOTTOM_BAR -> "Barra inferior únicamente"
                    NavigationStyle.DRAWER -> "Menú lateral desplegable (Drawer)"
                    NavigationStyle.HYBRID -> "Híbrido (Ambos combinados)"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStyleSelected(style) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentStyle == style,
                        onClick = { onStyleSelected(style) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, fontSize = 13.sp, color = GalleryTheme.colors.onSurface)
                }
            }
        }
    }
}

@Composable
fun UtilityRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = GalleryTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = GalleryTheme.colors.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GalleryTheme.colors.onSurface)
                Text(text = subtitle, fontSize = 12.sp, color = GalleryTheme.colors.onSurface.copy(alpha = 0.6f))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = GalleryTheme.colors.onSurface.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorDialog(
    details: ProfileWithDetails,
    availableThemes: List<com.gallery.privatevault.core.model.ThemeConfig>,
    onSave: (id: Long, name: String, isProtected: Boolean, pin: String?, isReadOnly: Boolean, themeId: String?, includeAll: Boolean) -> Unit,
    onDelete: (com.gallery.privatevault.core.database.entity.ProfileEntity) -> Unit,
    onAddRule: (profileId: Long, folderPath: String, ruleType: RuleType) -> Unit,
    onRemoveRule: (ruleId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(details.profile.name) }
    var isProtected by remember { mutableStateOf(details.profile.isProtected) }
    var pin by remember { mutableStateOf("") }
    var isReadOnly by remember { mutableStateOf(details.profile.isReadOnly) }
    var includeAll by remember { mutableStateOf(details.profile.includeAllByDefault) }
    var selectedThemeId by remember { mutableStateOf(details.profile.linkedThemeId) }

    var newRulePath by remember { mutableStateOf("") }
    var newRuleType by remember { mutableStateOf(RuleType.EXCLUDED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (details.profile.id == 0L) "Crear Perfil" else "Editar Perfil: ${details.profile.name}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del perfil") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Incluir todo el dispositivo", fontSize = 13.sp)
                        Switch(checked = includeAll, onCheckedChange = { includeAll = it })
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Proteger con Biometría / PIN", fontSize = 13.sp)
                        Switch(checked = isProtected, onCheckedChange = { isProtected = it })
                    }
                }

                if (isProtected) {
                    item {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("PIN de seguridad (opcional si usa huella)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Modo Solo Lectura", fontSize = 13.sp)
                        Switch(checked = isReadOnly, onCheckedChange = { isReadOnly = it })
                    }
                }

                // Reglas de carpetas
                if (details.profile.id != 0L) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Reglas de Carpetas (Exclusión gana siempre):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GalleryTheme.colors.primary)
                    }

                    items(details.rules) { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (rule.ruleType == RuleType.EXCLUDED) Color(0xFFFF5252).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.folderPath, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(if (rule.ruleType == RuleType.EXCLUDED) "EXCLUIDA" else "INCLUIDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (rule.ruleType == RuleType.EXCLUDED) Color(0xFFFF5252) else Color(0xFF4CAF50))
                            }
                            IconButton(onClick = { onRemoveRule(rule.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Eliminar regla", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newRulePath,
                                onValueChange = { newRulePath = it },
                                placeholder = { Text("Ruta (ej. DCIM/Capturas)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    onAddRule(details.profile.id, newRulePath, newRuleType)
                                    newRulePath = ""
                                },
                                enabled = newRulePath.isNotBlank()
                            ) {
                                Text("Añadir")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(details.profile.id, name, isProtected, pin.ifBlank { null }, isReadOnly, selectedThemeId, includeAll)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
