package com.nannymeals.app.ui.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.domain.model.MealReport
import com.nannymeals.app.domain.model.ReportPeriod
import com.nannymeals.app.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.LAST_WEEK,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val report: MealReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val exportedFileUri: Uri? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        generateReport()
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        if (period != ReportPeriod.CUSTOM) {
            generateReport()
        }
    }

    fun setCustomStartDate(date: LocalDate) {
        _uiState.update { 
            it.copy(
                customStartDate = date, 
                showStartDatePicker = false
            ) 
        }
        if (_uiState.value.customEndDate != null) {
            generateReport()
        }
    }

    fun setCustomEndDate(date: LocalDate) {
        _uiState.update { 
            it.copy(
                customEndDate = date, 
                showEndDatePicker = false
            ) 
        }
        if (_uiState.value.customStartDate != null) {
            generateReport()
        }
    }

    fun showStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = true) }
    }

    fun dismissStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = false) }
    }

    fun showEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = true) }
    }

    fun dismissEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = false) }
    }

    fun generateReport() {
        val state = _uiState.value
        val (startDate, endDate) = getDateRange(state)
        
        if (startDate == null || endDate == null) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val report = mealRepository.generateReport(startDate, endDate)
                _uiState.update { it.copy(report = report, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Échec de la génération du rapport"
                    ) 
                }
            }
        }
    }

    private fun getDateRange(state: ReportsUiState): Pair<LocalDate?, LocalDate?> {
        val today = LocalDate.now()
        return when (state.selectedPeriod) {
            ReportPeriod.LAST_WEEK -> today.minusDays(7) to today
            ReportPeriod.LAST_MONTH -> today.minusMonths(1) to today
            ReportPeriod.LAST_THREE_MONTHS -> today.minusMonths(3) to today
            ReportPeriod.CUSTOM -> state.customStartDate to state.customEndDate
        }
    }

    fun exportToCsv(context: Context) {
        val report = _uiState.value.report ?: return
        
        viewModelScope.launch {
            try {
                val file = createCsvReport(context, report)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                _uiState.update { it.copy(exportedFileUri = uri) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Échec de l'export CSV : ${e.message}") 
                }
            }
        }
    }

    private fun createCsvReport(context: Context, report: MealReport): File {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val fileName = "meal_report_${report.startDate.format(dateFormatter)}_to_${report.endDate.format(dateFormatter)}.csv"
        
        val reportsDir = File(context.cacheDir, "reports")
        reportsDir.mkdirs()
        val file = File(reportsDir, fileName)
        
        FileWriter(file).use { writer ->
            // Header
            writer.append("Rapport NannyMeals\n")
            writer.append("Période: ${report.startDate.format(dateFormatter)} au ${report.endDate.format(dateFormatter)}\n\n")
            
            // Summary
            writer.append("Résumé\n")
            writer.append("Total des repas,${report.totalMeals}\n")
            writer.append("Moyenne de repas par jour,${String.format("%.1f", report.averageMealsPerDay)}\n\n")
            
            // Meal Types
            writer.append("Répartition par type de repas\n")
            writer.append("Type,Nombre\n")
            MealType.values().forEach { type ->
                val count = report.mealTypeCounts[type] ?: 0
                writer.append("${type.name},${count}\n")
            }
            writer.append("\n")
            
            // Daily Breakdown
            writer.append("Repas quotidiens\n")
            writer.append("Date,Nombre\n")
            report.mealsPerDay.forEach { (date, count) ->
                writer.append("${date.format(dateFormatter)},${count}\n")
            }
            writer.append("\n")
            
            // Per Child
            writer.append("Repas par enfant\n")
            writer.append("Enfant,Nombre\n")
            report.childMealCounts.forEach { (child, count) ->
                writer.append("${child.name},${count}\n")
            }
            writer.append("\n")
            
            // Insights
            writer.append("Observations\n")
            report.insights.forEach { insight ->
                writer.append("$insight\n")
            }
            writer.append("\n")
            
            // Recommendations
            writer.append("Recommandations\n")
            report.recommendations.forEach { rec ->
                writer.append("$rec\n")
            }
        }
        
        return file
    }

    fun shareReport(context: Context) {
        val uri = _uiState.value.exportedFileUri ?: return
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rapport NannyMeals")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Partager le rapport"))
    }

    fun sendReportByEmail(context: Context, emails: List<String>) {
        val uri = _uiState.value.exportedFileUri ?: return
        val report = _uiState.value.report ?: return
        
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        val subject = "Rapport de repas : ${report.startDate.format(dateFormatter)} - ${report.endDate.format(dateFormatter)}"
        val body = buildString {
            appendLine("Bonjour,")
            appendLine()
            appendLine("Veuillez trouver ci-joint le rapport des repas pour la période du ${report.startDate.format(dateFormatter)} au ${report.endDate.format(dateFormatter)}.")
            appendLine()
            appendLine("Résumé :")
            appendLine("- Total des repas enregistrés : ${report.totalMeals}")
            appendLine("- Moyenne de repas par jour : ${String.format("%.1f", report.averageMealsPerDay)}")
            appendLine()
            appendLine("Cordialement,")
            appendLine("NannyMeals")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Envoyer par e-mail"))
    }

    fun clearExportedFile() {
        _uiState.update { it.copy(exportedFileUri = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
