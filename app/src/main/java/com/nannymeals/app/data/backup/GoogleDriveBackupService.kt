package com.nannymeals.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.nannymeals.app.data.database.NannyMealsDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for backing up and restoring the Room database to/from Google Drive.
 * Uses the Google Drive REST API to store the database file in the app's private folder.
 */
@Singleton
class GoogleDriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BACKUP_FILE_NAME = "nannymeals_backup.db"
        private const val BACKUP_MIME_TYPE = "application/x-sqlite3"
        private const val APP_FOLDER_NAME = "NannyMeals Backups"
    }

    private var driveService: Drive? = null
    private var googleSignInClient: GoogleSignInClient? = null

    /**
     * Creates the Google Sign-In client with Drive file scope.
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        if (googleSignInClient == null) {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
        }
        return googleSignInClient!!
    }

    /**
     * Returns the sign-in intent for Google authentication with Drive permissions.
     */
    fun getSignInIntent(): Intent {
        return getGoogleSignInClient().signInIntent
    }

    /**
     * Checks if the user is already signed in with Drive permissions.
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
    }

    /**
     * Gets the signed-in user's email address.
     */
    fun getSignedInEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    /**
     * Initializes the Drive service with the signed-in account.
     */
    fun initializeDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("NannyMeals")
            .build()
    }

    /**
     * Signs out from Google Drive.
     */
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            googleSignInClient?.signOut()
            driveService = null
        }
    }

    /**
     * Backs up the database to Google Drive.
     * @return BackupResult indicating success or failure
     */
    suspend fun backupDatabase(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext BackupResult.Error("Non connecté à Google Drive")

            // Get the database file
            val dbFile = context.getDatabasePath(NannyMealsDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                return@withContext BackupResult.Error("Base de données introuvable")
            }

            // Close the database checkpoint to ensure all data is written
            // This is important to ensure WAL data is written to the main database file

            // Find or create the app folder
            val folderId = getOrCreateAppFolder(drive)

            // Check if backup file already exists
            val existingFileId = findBackupFile(drive, folderId)

            // Create file metadata
            val fileMetadata = File().apply {
                name = BACKUP_FILE_NAME
                mimeType = BACKUP_MIME_TYPE
                if (existingFileId == null) {
                    parents = listOf(folderId)
                }
            }

            // Upload the database file
            val mediaContent = FileContent(BACKUP_MIME_TYPE, dbFile)

            val uploadedFile = if (existingFileId != null) {
                // Update existing file
                drive.files().update(existingFileId, fileMetadata, mediaContent)
                    .setFields("id, name, modifiedTime")
                    .execute()
            } else {
                // Create new file
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, modifiedTime")
                    .execute()
            }

            val modifiedTime = uploadedFile.modifiedTime?.value ?: System.currentTimeMillis()
            BackupResult.Success(modifiedTime)
        } catch (e: IOException) {
            BackupResult.Error("Erreur réseau: ${e.message}")
        } catch (e: Exception) {
            BackupResult.Error("Échec de la sauvegarde: ${e.message}")
        }
    }

    /**
     * Restores the database from Google Drive.
     * @return BackupResult indicating success or failure
     */
    suspend fun restoreDatabase(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext BackupResult.Error("Non connecté à Google Drive")

            // Find the app folder
            val folderId = findAppFolder(drive)
                ?: return@withContext BackupResult.Error("Aucune sauvegarde trouvée")

            // Find the backup file
            val fileId = findBackupFile(drive, folderId)
                ?: return@withContext BackupResult.Error("Aucune sauvegarde trouvée")

            // Get the database file path
            val dbFile = context.getDatabasePath(NannyMealsDatabase.DATABASE_NAME)
            val dbDir = dbFile.parentFile
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs()
            }

            // Download the backup file to a temp location first
            val tempFile = java.io.File(context.cacheDir, "restore_temp.db")
            
            FileOutputStream(tempFile).use { outputStream ->
                drive.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }

            // Delete old database files (including WAL and SHM files)
            val walFile = java.io.File(dbFile.path + "-wal")
            val shmFile = java.io.File(dbFile.path + "-shm")
            
            dbFile.delete()
            walFile.delete()
            shmFile.delete()

            // Move temp file to database location
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            BackupResult.Success(System.currentTimeMillis())
        } catch (e: IOException) {
            BackupResult.Error("Erreur réseau: ${e.message}")
        } catch (e: Exception) {
            BackupResult.Error("Échec de la restauration: ${e.message}")
        }
    }

    /**
     * Gets information about the last backup.
     * @return BackupInfo or null if no backup exists
     */
    suspend fun getBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            val folderId = findAppFolder(drive) ?: return@withContext null
            val fileId = findBackupFile(drive, folderId) ?: return@withContext null

            val file = drive.files().get(fileId)
                .setFields("id, name, modifiedTime, size")
                .execute()

            BackupInfo(
                lastBackupTime = file.modifiedTime?.value ?: 0L,
                sizeBytes = file.getSize()?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds or creates the app folder in Google Drive.
     */
    private fun getOrCreateAppFolder(drive: Drive): String {
        // First, try to find existing folder
        findAppFolder(drive)?.let { return it }

        // Create new folder
        val folderMetadata = File().apply {
            name = APP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()

        return folder.id
    }

    /**
     * Finds the app folder in Google Drive.
     */
    private fun findAppFolder(drive: Drive): String? {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$APP_FOLDER_NAME' and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    /**
     * Finds the backup file in the app folder.
     */
    private fun findBackupFile(drive: Drive, folderId: String): String? {
        val query = "'$folderId' in parents and name='$BACKUP_FILE_NAME' and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        return result.files.firstOrNull()?.id
    }
}

/**
 * Result of a backup or restore operation.
 */
sealed class BackupResult {
    data class Success(val timestamp: Long) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/**
 * Information about the last backup.
 */
data class BackupInfo(
    val lastBackupTime: Long,
    val sizeBytes: Long
) {
    val formattedTime: String
        get() {
            val instant = Instant.ofEpochMilli(lastBackupTime)
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault())
            return formatter.format(instant)
        }

    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> "${sizeBytes / (1024 * 1024)} MB"
            }
        }
}
