@file:OptIn(ExperimentalMaterial3Api::class)

package com.nannymeals.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.domain.model.MealReport
import com.nannymeals.app.domain.model.ReportPeriod
import com.nannymeals.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    var showEmailDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportedFileUri) {
        if (uiState.exportedFileUri != null) {
            // File was exported successfully
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rapports") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selection
            item {
                PeriodSelectionCard(
                    selectedPeriod = uiState.selectedPeriod,
                    customStartDate = uiState.customStartDate,
                    customEndDate = uiState.customEndDate,
                    onPeriodSelected = viewModel::selectPeriod,
                    onShowStartDatePicker = viewModel::showStartDatePicker,
                    onShowEndDatePicker = viewModel::showEndDatePicker,
                    dateFormatter = dateFormatter
                )
            }

            // Report Content
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                uiState.report == null -> {
                    item {
                        EmptyReportState()
                    }
                }
                uiState.report?.totalMeals == 0 -> {
                    item {
                        NoDataState()
                    }
                }
                else -> {
                    val report = uiState.report!!
                    
                    // Summary Card
                    item {
                        SummaryCard(report)
                    }
                    
                    // Meal Type Breakdown
                    item {
                        MealTypeBreakdownCard(report)
                    }
                    
                    // Per Child Breakdown
                    if (report.childMealCounts.isNotEmpty()) {
                        item {
                            ChildMealBreakdownCard(report)
                        }
                    }
                    
                    // Insights
                    if (report.insights.isNotEmpty()) {
                        item {
                            InsightsCard(report.insights)
                        }
                    }
                    
                    // Recommendations
                    if (report.recommendations.isNotEmpty()) {
                        item {
                            RecommendationsCard(report.recommendations)
                        }
                    }
                    
                    // Export Actions
                    item {
                        ExportActionsCard(
                            onExportCsv = { viewModel.exportToCsv(context) },
                            onShare = { viewModel.shareReport(context) },
                            onEmail = { showEmailDialog = true },
                            hasExportedFile = uiState.exportedFileUri != null
                        )
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = viewModel::clearError) {
                                Text("Fermer")
                            }
                            }
                        }
                    }
                }
            }
        }

        // Date Pickers
        if (uiState.showStartDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = (uiState.customStartDate ?: LocalDate.now().minusWeeks(1))
                    .toEpochDay() * 86400000
            )
            
            DatePickerDialog(
                onDismissRequest = viewModel::dismissStartDatePicker,
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = LocalDate.ofEpochDay(millis / 86400000)
                                viewModel.setCustomStartDate(date)
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissStartDatePicker) {
                        Text("Annuler")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (uiState.showEndDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = (uiState.customEndDate ?: LocalDate.now())
                    .toEpochDay() * 86400000
            )
            
            DatePickerDialog(
                onDismissRequest = viewModel::dismissEndDatePicker,
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = LocalDate.ofEpochDay(millis / 86400000)
                                viewModel.setCustomEndDate(date)
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissEndDatePicker) {
                        Text("Annuler")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Email Dialog
        if (showEmailDialog) {
            var emailInput by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showEmailDialog = false },
                title = { Text("Envoyer le rapport") },
                text = {
                    Column {
                        Text("Entrez les adresses e-mail (séparées par des virgules) :")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("parent@email.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val emails = emailInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (emails.isNotEmpty()) {
                                viewModel.sendReportByEmail(context, emails)
                            }
                            showEmailDialog = false
                        }
                    ) {
                        Text("Envoyer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmailDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }

@Composable
fun PeriodSelectionCard(
    selectedPeriod: ReportPeriod,
    customStartDate: LocalDate?,
    customEndDate: LocalDate?,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    dateFormatter: DateTimeFormatter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Période du rapport",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.LAST_WEEK,
                    onClick = { onPeriodSelected(ReportPeriod.LAST_WEEK) },
                    label = { Text("Semaine") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.LAST_MONTH,
                    onClick = { onPeriodSelected(ReportPeriod.LAST_MONTH) },
                    label = { Text("Mois") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.LAST_THREE_MONTHS,
                    onClick = { onPeriodSelected(ReportPeriod.LAST_THREE_MONTHS) },
                    label = { Text("3 Mois") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.CUSTOM,
                    onClick = { onPeriodSelected(ReportPeriod.CUSTOM) },
                    label = { Text("Personnalisé") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (selectedPeriod == ReportPeriod.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShowStartDatePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(customStartDate?.format(dateFormatter) ?: "Date de début")
                    }
                    
                    OutlinedButton(
                        onClick = onShowEndDatePicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(customEndDate?.format(dateFormatter) ?: "Date de fin")
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(report: MealReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Résumé",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = report.totalMeals.toString(),
                    label = "Total repas",
                    icon = Icons.Default.Restaurant
                )
                
                StatItem(
                    value = String.format("%.1f", report.averageMealsPerDay),
                    label = "Moy./Jour",
                    icon = Icons.Default.TrendingUp
                )
                
                StatItem(
                    value = report.mealsPerDay.size.toString(),
                    label = "Jours enr.",
                    icon = Icons.Default.CalendarMonth
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MealTypeBreakdownCard(report: MealReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Répartition par type de repas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MealType.values().forEach { type ->
                val count = report.mealTypeCounts[type] ?: 0
                val percentage = if (report.totalMeals > 0) {
                    (count.toFloat() / report.totalMeals * 100)
                } else 0f
                
                val color = when (type) {
                    MealType.LUNCH -> LunchColor
                    MealType.SNACK -> SnackColor
                }
                
                val typeName = when (type) {
                    MealType.LUNCH -> "Déjeuner"
                    MealType.SNACK -> "Collation"
                }
                
                MealTypeProgressBar(
                    type = typeName,
                    count = count,
                    percentage = percentage,
                    color = color
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MealTypeProgressBar(
    type: String,
    count: Int,
    percentage: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(type, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$count (${String.format("%.0f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ChildMealBreakdownCard(report: MealReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Repas par enfant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            report.childMealCounts.forEach { (child, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(child.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "$count repas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InsightsCard(insights: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Observations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.forEach { insight ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(insight, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommandations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(recommendation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ExportActionsCard(
    onExportCsv: () -> Unit,
    onShare: () -> Unit,
    onEmail: () -> Unit,
    hasExportedFile: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Exporter et partager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV")
                }
                
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    enabled = hasExportedFile
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Partager")
                }
                
                OutlinedButton(
                    onClick = onEmail,
                    modifier = Modifier.weight(1f),
                    enabled = hasExportedFile
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("E-mail")
                }
            }
        }
    }
}

@Composable
fun EmptyReportState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Sélectionnez une période pour générer le rapport",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun NoDataState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Aucun repas enregistré pour cette période",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Commencez à enregistrer des repas pour voir les rapports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
