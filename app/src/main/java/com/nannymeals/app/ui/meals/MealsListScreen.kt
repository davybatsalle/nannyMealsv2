package com.nannymeals.app.ui.meals

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsListScreen(
    onNavigateToAddMeal: (LocalDate) -> Unit,
    onNavigateToEditMeal: (Long) -> Unit,
    viewModel: MealsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mealsForSelectedDate by viewModel.mealsForSelectedDate.collectAsState()
    val mealsForMonth by viewModel.mealsForMonth.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repas") },
                actions = {
                    IconButton(onClick = viewModel::goToToday) {
                        Icon(Icons.Default.Today, contentDescription = "Aujourd'hui")
                    }
                    IconButton(onClick = viewModel::toggleCalendarView) {
                        Icon(
                            imageVector = if (uiState.showCalendar) Icons.Default.ViewList else Icons.Default.CalendarMonth,
                            contentDescription = "Changer de vue"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddMeal(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un repas")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Calendar View
            if (uiState.showCalendar) {
                CalendarView(
                    currentMonth = uiState.currentMonth,
                    selectedDate = uiState.selectedDate,
                    datesWithMeals = viewModel.getDatesWithMeals(),
                    onDateSelected = viewModel::selectDate,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth
                )
            }

            // Selected Date Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share to Facebook button - only show when there are meals
                    if (mealsForSelectedDate.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val shareText = viewModel.generateShareText()
                                if (shareText != null) {
                                    shareToFacebook(context, shareText)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Partager sur Facebook",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (uiState.selectedDate == LocalDate.now()) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Aujourd'hui") },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Meals List
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    mealsForSelectedDate.isEmpty() -> {
                        EmptyMealsState(
                            modifier = Modifier.align(Alignment.Center),
                            onAddMeal = { onNavigateToAddMeal(uiState.selectedDate) }
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(mealsForSelectedDate, key = { it.id }) { meal ->
                                MealCard(
                                    meal = meal,
                                    onClick = { onNavigateToEditMeal(meal.id) },
                                    onDelete = { viewModel.showDeleteConfirmation(meal) }
                                )
                            }
                        }
                    }
                }

                // Delete confirmation dialog
                uiState.mealToDelete?.let { meal ->
                    AlertDialog(
                        onDismissRequest = viewModel::dismissDeleteConfirmation,
                        title = { Text("Supprimer le repas") },
                        text = { Text("Êtes-vous sûr de vouloir supprimer cette entrée de ${meal.mealTypeDisplay.lowercase()} ?") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.deleteMeal(meal.id) }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissDeleteConfirmation) {
                                Text("Annuler")
                            }
                        }
                    )
                }

                // Error snackbar
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = viewModel::clearError) {
                                Text("Fermer")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithMeals: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    
    Column(modifier = modifier.padding(16.dp)) {
        // Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
            }
            
            Text(
                text = currentMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Day of Week Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar Grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
        val daysInMonth = currentMonth.lengthOfMonth()
        
        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
        val days = (1..totalCells).map { cell ->
            val dayOffset = cell - firstDayOfWeek - 1
            if (dayOffset in 0 until daysInMonth) {
                firstDayOfMonth.plusDays(dayOffset.toLong())
            } else {
                null
            }
        }
        
        Column {
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        CalendarDay(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            hasMeals = date in datesWithMeals,
                            onClick = { date?.let { onDateSelected(it) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    hasMeals: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            date?.let {
                Text(
                    text = it.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (hasMeals) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mealColor = when (meal.mealType) {
        MealType.LUNCH -> LunchColor
        MealType.SNACK -> SnackColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(mealColor)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.mealTypeDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = meal.formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (meal.children.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(meal.children) { child ->
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        child.name, 
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
                
                if (meal.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = meal.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyMealsState(
    modifier: Modifier = Modifier,
    onAddMeal: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Aucun repas enregistré",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Appuyez sur + pour ajouter un repas pour ce jour",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onAddMeal) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter un repas")
        }
    }
}

/**
 * Shares the meal summary text to Facebook.
 * Tries to open the Facebook app directly, falls back to a general share intent.
 */
private fun shareToFacebook(context: android.content.Context, text: String) {
    try {
        // Try to share directly to Facebook app
        val facebookIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.facebook.katana") // Facebook app package name
        }
        
        // Check if Facebook is installed
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(facebookIntent, 0)
        
        if (activities.isNotEmpty()) {
            context.startActivity(facebookIntent)
        } else {
            // Facebook not installed, use a general share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager les repas"))
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Impossible de partager. Veuillez réessayer.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
