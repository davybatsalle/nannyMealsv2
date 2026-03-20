@file:OptIn(ExperimentalMaterial3Api::class)

package com.nannymeals.app.ui.meals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.model.FoodItem
import com.nannymeals.app.domain.model.MealItem
import com.nannymeals.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: MealFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val children by viewModel.children.collectAsState()
    val frequentItems by viewModel.frequentItems.collectAsState()
    val suggestedItems by viewModel.suggestedItems.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Modifier le repas" else "Ajouter un repas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && viewModel.isEditing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date and Time Section
                    Text(
                        text = "Date et heure",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Date picker
                        OutlinedTextField(
                            value = uiState.date.format(dateFormatter),
                            onValueChange = { },
                            label = { Text("Date") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.showDatePicker() },
                            enabled = !uiState.isLoading
                        )
                        
                        // Time picker
                        OutlinedTextField(
                            value = uiState.time.format(timeFormatter),
                            onValueChange = { },
                            label = { Text("Heure") },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.showTimePicker() },
                            enabled = !uiState.isLoading
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Meal Type Section
                    Text(
                        text = "Type de repas *",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.values().forEach { mealType ->
                            MealTypeChip(
                                mealType = mealType,
                                isSelected = uiState.mealType == mealType,
                                onClick = { viewModel.onMealTypeChange(mealType) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Children Selection Section
                    Text(
                        text = "Enfants *",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (children.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aucun enfant ajouté",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Ajoutez d'abord des enfants pour enregistrer les repas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            children.forEach { child ->
                                ChildSelectionItem(
                                    child = child,
                                    isSelected = child.id in uiState.selectedChildIds,
                                    onClick = { viewModel.toggleChildSelection(child.id) },
                                    enabled = !uiState.isLoading
                                )
                            }
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // ===== MEAL ITEMS SECTION =====
                    Text(
                        text = "Qu'est-ce qui est servi ?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Added items
                    if (uiState.mealItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.mealItems.forEach { item ->
                                MealItemChip(
                                    item = item,
                                    onRemove = { viewModel.removeMealItem(item) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Add item button
                    OutlinedButton(
                        onClick = viewModel::showAddItemDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter un aliment")
                    }
                    
                    // Quick add - Frequent items
                    if (frequentItems.isNotEmpty()) {
                        Text(
                            text = "Fréquemment utilisés",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(frequentItems.take(8)) { item ->
                                SuggestionChip(
                                    onClick = { viewModel.addMealItemFromFoodItem(item) },
                                    label = { Text(item.name) },
                                    icon = {
                                        Icon(
                                            getCategoryIcon(item.category),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // Variety suggestions
                    if (suggestedItems.isNotEmpty()) {
                        Text(
                            text = "💡 Essayez quelque chose de différent",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestedItems.take(6)) { item ->
                                SuggestionChip(
                                    onClick = { viewModel.addMealItemFromFoodItem(item) },
                                    label = { Text(item.name) },
                                    icon = {
                                        Icon(
                                            getCategoryIcon(item.category),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Notes Section
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Ajouter des notes (optionnel)") },
                        placeholder = { Text("Ex: L'enfant a apprécié le repas, restes, etc.") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Error message
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Save button
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isLoading && children.isNotEmpty()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (viewModel.isEditing) "Mettre à jour" else "Enregistrer")
                        }
                    }
                }
            }

            // Date Picker Dialog
            if (uiState.showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date.toEpochDay() * 86400000
                )
                
                DatePickerDialog(
                    onDismissRequest = viewModel::dismissDatePicker,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = LocalDate.ofEpochDay(millis / 86400000)
                                    viewModel.onDateChange(date)
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissDatePicker) {
                            Text("Annuler")
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Time Picker Dialog
            if (uiState.showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = uiState.time.hour,
                    initialMinute = uiState.time.minute
                )
                
                AlertDialog(
                    onDismissRequest = viewModel::dismissTimePicker,
                    title = { Text("Sélectionner l'heure") },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.onTimeChange(
                                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                                )
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissTimePicker) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Add Item Dialog
            if (uiState.showAddItemDialog) {
                AddFoodItemDialog(
                    searchQuery = uiState.itemSearchQuery,
                    onSearchQueryChange = viewModel::onItemSearchQueryChange,
                    searchResults = searchResults,
                    frequentItems = frequentItems,
                    onSelectItem = { viewModel.addMealItemFromFoodItem(it) },
                    onAddCustomItem = { name, category -> 
                        viewModel.addCustomMealItem(name, category) 
                    },
                    onDismiss = viewModel::dismissAddItemDialog
                )
            }
        }
    }
}

@Composable
fun MealTypeChip(
    mealType: MealType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = when (mealType) {
        MealType.LUNCH -> LunchColor
        MealType.SNACK -> SnackColor
    }
    
    val icon = when (mealType) {
        MealType.LUNCH -> Icons.Default.LunchDining
        MealType.SNACK -> Icons.Default.Cookie
    }
    
    val label = when (mealType) {
        MealType.LUNCH -> "Déjeuner"
        MealType.SNACK -> "Collation"
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { 
            Text(
                label,
                style = MaterialTheme.typography.labelSmall
            ) 
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else color
            )
        },
        modifier = modifier,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun ChildSelectionItem(
    child: Child,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick
            ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    enabled = enabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (child.hasAllergies || child.hasDietaryRestrictions) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (child.hasAllergies) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "A des allergies",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            if (child.hasDietaryRestrictions) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = "A des restrictions alimentaires",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
            
            Text(
                text = child.ageDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MealItemChip(
    item: MealItem,
    onRemove: () -> Unit
) {
    InputChip(
        selected = true,
        onClick = { },
        label = { 
            Text(
                if (item.portion.isNotEmpty()) "${item.name} (${item.portion})" else item.name
            )
        },
        leadingIcon = {
            Icon(
                getCategoryIcon(item.category),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Retirer",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
fun AddFoodItemDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<FoodItem>,
    frequentItems: List<FoodItem>,
    onSelectItem: (FoodItem) -> Unit,
    onAddCustomItem: (String, FoodCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(FoodCategory.PROTEIN) }
    var customItemName by remember { mutableStateOf("") }
    var showCustomItemForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un aliment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Rechercher des aliments") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showCustomItemForm) {
                    // Custom item form
                    Text(
                        text = "Ajouter un aliment personnalisé",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Nom de l'aliment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Catégorie :", style = MaterialTheme.typography.labelSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(FoodCategory.values().toList()) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomItemForm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (customItemName.isNotBlank()) {
                                    onAddCustomItem(customItemName.trim(), selectedCategory)
                                    onDismiss()
                                }
                            },
                            enabled = customItemName.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ajouter")
                        }
                    }
                } else {
                    // Search results or suggestions
                    if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                        Text(
                            text = "Résultats de recherche",
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(searchResults) { item ->
                                FoodItemRow(
                                    item = item,
                                    onClick = {
                                        onSelectItem(item)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                        // No results, offer to add custom
                        Text(
                            text = "Aucun aliment trouvé pour \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = {
                                customItemName = searchQuery
                                showCustomItemForm = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter \"$searchQuery\" comme nouvel aliment")
                        }
                    } else {
                        // Show frequent items
                        if (frequentItems.isNotEmpty()) {
                            Text(
                                text = "Ajout rapide",
                                style = MaterialTheme.typography.labelMedium
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                items(frequentItems.take(10)) { item ->
                                    FoodItemRow(
                                        item = item,
                                        onClick = {
                                            onSelectItem(item)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Add custom button
                    TextButton(
                        onClick = { showCustomItemForm = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter un aliment personnalisé")
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun FoodItemRow(
    item: FoodItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getCategoryIcon(item.category),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = item.categoryDisplay,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getCategoryIcon(category: FoodCategory) = when (category) {
    FoodCategory.PROTEIN -> Icons.Default.SetMeal
    FoodCategory.GRAIN -> Icons.Default.BakeryDining
    FoodCategory.VEGETABLE -> Icons.Default.Spa
    FoodCategory.FRUIT -> Icons.Default.LocalFlorist
    FoodCategory.DAIRY -> Icons.Default.LocalDrink
    FoodCategory.DRINK -> Icons.Default.LocalCafe
    FoodCategory.SNACK -> Icons.Default.Cookie
    FoodCategory.OTHER -> Icons.Default.Restaurant
}
