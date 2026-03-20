package com.nannymeals.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    var hasNotificationPermission by remember { mutableStateOf(true) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Google Sign-In launcher for Drive backup
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        viewModel.handleSignInResult(result)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Notifications Section
            SettingsSection(title = "Rappels de repas") {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Activez les notifications dans les paramètres pour recevoir les rappels de repas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Lunch Reminder
                ReminderSettingItem(
                    title = "Rappel déjeuner",
                    icon = Icons.Default.LunchDining,
                    isEnabled = reminderSettings.lunchEnabled,
                    time = LocalTime.of(reminderSettings.lunchHour, reminderSettings.lunchMinute),
                    timeFormatter = timeFormatter,
                    onToggle = { viewModel.toggleLunchReminder(it) },
                    onTimeClick = { viewModel.showLunchTimePicker() }
                )

                // Snack Reminder
                ReminderSettingItem(
                    title = "Rappel collation",
                    icon = Icons.Default.Cookie,
                    isEnabled = reminderSettings.snackEnabled,
                    time = LocalTime.of(reminderSettings.snackHour, reminderSettings.snackMinute),
                    timeFormatter = timeFormatter,
                    onToggle = { viewModel.toggleSnackReminder(it) },
                    onTimeClick = { viewModel.showSnackTimePicker() }
                )
            }

            // Google Drive Backup Section
            SettingsSection(title = "Sauvegarde Google Drive") {
                if (!uiState.isSignedInToDrive) {
                    // Not signed in - show connect button
                    ListItem(
                        headlineContent = { Text("Connecter Google Drive") },
                        supportingContent = { Text("Sauvegardez vos données sur Google Drive") },
                        leadingContent = {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Button(
                                onClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) }
                            ) {
                                Text("Connecter")
                            }
                        }
                    )
                } else {
                    // Signed in - show backup options
                    ListItem(
                        headlineContent = { Text("Compte Google Drive") },
                        supportingContent = { Text(uiState.driveAccountEmail ?: "") },
                        leadingContent = {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            TextButton(onClick = viewModel::signOutFromDrive) {
                                Text("Déconnecter")
                            }
                        }
                    )

                    Divider()

                    // Last backup info
                    if (uiState.backupInfo != null) {
                        ListItem(
                            headlineContent = { Text("Dernière sauvegarde") },
                            supportingContent = { 
                                Text("${uiState.backupInfo!!.formattedTime} (${uiState.backupInfo!!.formattedSize})") 
                            },
                            leadingContent = {
                                Icon(Icons.Default.History, contentDescription = null)
                            }
                        )
                        Divider()
                    }

                    // Backup button
                    ListItem(
                        headlineContent = { Text("Sauvegarder maintenant") },
                        supportingContent = { Text("Enregistrer la base de données sur Google Drive") },
                        leadingContent = {
                            Icon(Icons.Default.Backup, contentDescription = null)
                        },
                        trailingContent = {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(onClick = viewModel::backupToGoogleDrive) {
                                    Icon(Icons.Default.Upload, contentDescription = "Sauvegarder")
                                }
                            }
                        },
                        modifier = Modifier.clickable(
                            enabled = !uiState.isBackingUp,
                            onClick = viewModel::backupToGoogleDrive
                        )
                    )

                    Divider()

                    // Restore button
                    ListItem(
                        headlineContent = { Text("Restaurer depuis la sauvegarde") },
                        supportingContent = { Text("Remplacer les données locales par la sauvegarde") },
                        leadingContent = {
                            Icon(Icons.Default.Restore, contentDescription = null)
                        },
                        trailingContent = {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(
                                    onClick = viewModel::showRestoreConfirmation,
                                    enabled = uiState.backupInfo != null
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Restaurer")
                                }
                            }
                        },
                        modifier = Modifier.clickable(
                            enabled = !uiState.isRestoring && uiState.backupInfo != null,
                            onClick = viewModel::showRestoreConfirmation
                        )
                    )
                }

                // Success/Error messages
                uiState.backupSuccess?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearBackupMessage) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                uiState.backupError?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                message, 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearBackupMessage) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // About Section
            SettingsSection(title = "À propos") {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
                
                Divider()
                
                ListItem(
                    headlineContent = { Text("Politique de confidentialité") },
                    leadingContent = {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { /* Open privacy policy */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Time Pickers
        if (uiState.showLunchTimePicker) {
            TimePickerDialog(
                initialHour = reminderSettings.lunchHour,
                initialMinute = reminderSettings.lunchMinute,
                onConfirm = { hour, minute -> viewModel.updateLunchTime(hour, minute) },
                onDismiss = viewModel::dismissTimePicker
            )
        }

        if (uiState.showSnackTimePicker) {
            TimePickerDialog(
                initialHour = reminderSettings.snackHour,
                initialMinute = reminderSettings.snackMinute,
                onConfirm = { hour, minute -> viewModel.updateSnackTime(hour, minute) },
                onDismiss = viewModel::dismissTimePicker
            )
        }

        // Restore Confirmation
        if (uiState.showRestoreConfirmation) {
            AlertDialog(
                onDismissRequest = viewModel::dismissRestoreConfirmation,
                title = { Text("Restaurer la sauvegarde") },
                text = { 
                    Text("Cette action remplacera toutes les données locales par celles de la sauvegarde. Cette opération est irréversible. Êtes-vous sûr ?") 
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::restoreFromGoogleDrive
                    ) {
                        Text("Restaurer", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissRestoreConfirmation) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ReminderSettingItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    time: LocalTime,
    timeFormatter: DateTimeFormatter,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            }
        )
        
        if (isEnabled) {
            ListItem(
                headlineContent = { Text("Heure") },
                supportingContent = { Text(time.format(timeFormatter)) },
                leadingContent = {
                    Spacer(modifier = Modifier.width(24.dp))
                },
                trailingContent = {
                    TextButton(onClick = onTimeClick) {
                        Text("Modifier")
                    }
                },
                modifier = Modifier.clickable(onClick = onTimeClick)
            )
        }
        
        Divider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner l'heure") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
