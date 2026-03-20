package com.nannymeals.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.nannymeals.app.data.backup.BackupInfo
import com.nannymeals.app.data.backup.BackupResult
import com.nannymeals.app.data.backup.GoogleDriveBackupService
import com.nannymeals.app.notifications.ReminderSettings
import com.nannymeals.app.notifications.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val showLunchTimePicker: Boolean = false,
    val showSnackTimePicker: Boolean = false,
    // Backup states
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupInfo: BackupInfo? = null,
    val backupError: String? = null,
    val backupSuccess: String? = null,
    val showRestoreConfirmation: Boolean = false,
    val isSignedInToDrive: Boolean = false,
    val driveAccountEmail: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val googleDriveBackupService: GoogleDriveBackupService
) : ViewModel() {

    private val settingsRepository = SettingsRepository(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val reminderSettings: StateFlow<ReminderSettings> = settingsRepository.reminderSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderSettings()
        )

    init {
        checkDriveSignInStatus()
    }

    // ==================== Google Drive Backup Methods ====================

    private fun checkDriveSignInStatus() {
        val isSignedIn = googleDriveBackupService.isSignedIn()
        val email = googleDriveBackupService.getSignedInEmail()
        _uiState.update { 
            it.copy(
                isSignedInToDrive = isSignedIn,
                driveAccountEmail = email
            ) 
        }
        if (isSignedIn) {
            loadBackupInfo()
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return googleDriveBackupService.getSignInIntent()
    }

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                googleDriveBackupService.initializeDriveService(account)
                _uiState.update { 
                    it.copy(
                        isSignedInToDrive = true,
                        driveAccountEmail = account.email
                    ) 
                }
                loadBackupInfo()
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    12500 -> "Configuration OAuth manquante. Configurez OAuth dans Google Cloud Console."
                    12501 -> "Connexion annulée par l'utilisateur"
                    12502 -> "Erreur réseau. Vérifiez votre connexion internet."
                    10 -> "Configuration développeur invalide. Vérifiez SHA-1 et package name."
                    else -> "Échec de la connexion (code: ${e.statusCode})"
                }
                _uiState.update { 
                    it.copy(backupError = errorMessage) 
                }
            }
        }
    }

    fun signOutFromDrive() {
        viewModelScope.launch {
            googleDriveBackupService.signOut()
            _uiState.update { 
                it.copy(
                    isSignedInToDrive = false,
                    driveAccountEmail = null,
                    backupInfo = null
                ) 
            }
        }
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            val info = googleDriveBackupService.getBackupInfo()
            _uiState.update { it.copy(backupInfo = info) }
        }
    }

    fun backupToGoogleDrive() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupError = null, backupSuccess = null) }
            
            when (val result = googleDriveBackupService.backupDatabase()) {
                is BackupResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isBackingUp = false,
                            backupSuccess = "Sauvegarde réussie"
                        ) 
                    }
                    loadBackupInfo()
                }
                is BackupResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isBackingUp = false,
                            backupError = result.message
                        ) 
                    }
                }
            }
        }
    }

    fun showRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmation = true) }
    }

    fun dismissRestoreConfirmation() {
        _uiState.update { it.copy(showRestoreConfirmation = false) }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isRestoring = true, 
                    backupError = null, 
                    backupSuccess = null,
                    showRestoreConfirmation = false
                ) 
            }
            
            when (val result = googleDriveBackupService.restoreDatabase()) {
                is BackupResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isRestoring = false,
                            backupSuccess = "Restauration réussie. Redémarrez l'application."
                        ) 
                    }
                }
                is BackupResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isRestoring = false,
                            backupError = result.message
                        ) 
                    }
                }
            }
        }
    }

    fun clearBackupMessage() {
        _uiState.update { it.copy(backupError = null, backupSuccess = null) }
    }

    // ==================== Reminder Methods ====================

    fun toggleLunchReminder(enabled: Boolean) {
        viewModelScope.launch {
            val settings = reminderSettings.value
            settingsRepository.updateLunchReminder(
                enabled,
                settings.lunchHour,
                settings.lunchMinute
            )
        }
    }

    fun updateLunchTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateLunchReminder(
                reminderSettings.value.lunchEnabled,
                hour,
                minute
            )
            _uiState.update { it.copy(showLunchTimePicker = false) }
        }
    }

    fun toggleSnackReminder(enabled: Boolean) {
        viewModelScope.launch {
            val settings = reminderSettings.value
            settingsRepository.updateSnackReminder(
                enabled,
                settings.snackHour,
                settings.snackMinute
            )
        }
    }

    fun updateSnackTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateSnackReminder(
                reminderSettings.value.snackEnabled,
                hour,
                minute
            )
            _uiState.update { it.copy(showSnackTimePicker = false) }
        }
    }

    fun showLunchTimePicker() {
        _uiState.update { it.copy(showLunchTimePicker = true) }
    }

    fun showSnackTimePicker() {
        _uiState.update { it.copy(showSnackTimePicker = true) }
    }

    fun dismissTimePicker() {
        _uiState.update { 
            it.copy(
                showLunchTimePicker = false,
                showSnackTimePicker = false
            ) 
        }
    }
}
