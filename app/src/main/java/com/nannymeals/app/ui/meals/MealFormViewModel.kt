package com.nannymeals.app.ui.meals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.model.FoodItem
import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.domain.model.MealItem
import com.nannymeals.app.domain.model.MealType
import com.nannymeals.app.domain.repository.ChildRepository
import com.nannymeals.app.domain.repository.FoodItemRepository
import com.nannymeals.app.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class MealFormUiState(
    val mealId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val mealType: MealType? = null,
    val notes: String = "",
    val selectedChildIds: Set<Long> = emptySet(),
    val mealItems: List<MealItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showAddItemDialog: Boolean = false,
    val itemSearchQuery: String = ""
)

@HiltViewModel
class MealFormViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val childRepository: ChildRepository,
    private val foodItemRepository: FoodItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mealId: Long? = savedStateHandle.get<Long>("mealId")?.takeIf { it > 0 }
    private val initialDate: LocalDate? = savedStateHandle.get<String>("date")?.let { 
        LocalDate.parse(it) 
    }

    private val _uiState = MutableStateFlow(
        MealFormUiState(
            mealId = mealId,
            date = initialDate ?: LocalDate.now()
        )
    )
    val uiState: StateFlow<MealFormUiState> = _uiState.asStateFlow()

    val children: StateFlow<List<Child>> = childRepository.getChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Most used food items for quick selection
    val frequentItems: StateFlow<List<FoodItem>> = foodItemRepository.getMostUsedItems(15)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Suggestions - items not used recently (for variety)
    val suggestedItems: StateFlow<List<FoodItem>> = foodItemRepository.getSuggestedItems(daysAgo = 7, limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results
    private val _searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<FoodItem>> = combine(
        _searchQuery,
        foodItemRepository.getAllFoodItems()
    ) { query, allItems ->
        if (query.isBlank()) {
            emptyList()
        } else {
            allItems.filter { it.name.contains(query, ignoreCase = true) }
                .take(20)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isEditing: Boolean = mealId != null

    init {
        // Initialize default food items on first launch
        viewModelScope.launch {
            foodItemRepository.initializeDefaultItems()
        }
        mealId?.let { loadMeal(it) }
    }

    private fun loadMeal(mealId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val meal = mealRepository.getMealById(mealId)
            meal?.let {
                _uiState.update { state ->
                    state.copy(
                        date = meal.date,
                        time = meal.time,
                        mealType = meal.mealType,
                        notes = meal.notes,
                        selectedChildIds = meal.children.map { it.id }.toSet(),
                        mealItems = meal.items,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date, showDatePicker = false, error = null) }
    }

    fun onTimeChange(time: LocalTime) {
        _uiState.update { it.copy(time = time, showTimePicker = false, error = null) }
    }

    fun onMealTypeChange(mealType: MealType) {
        _uiState.update { it.copy(mealType = mealType, error = null) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun toggleChildSelection(childId: Long) {
        _uiState.update { state ->
            val newSelection = if (childId in state.selectedChildIds) {
                state.selectedChildIds - childId
            } else {
                state.selectedChildIds + childId
            }
            state.copy(selectedChildIds = newSelection, error = null)
        }
    }

    // Meal Items management
    fun addMealItem(item: MealItem) {
        _uiState.update { state ->
            state.copy(
                mealItems = state.mealItems + item,
                showAddItemDialog = false,
                itemSearchQuery = ""
            )
        }
    }

    fun addMealItemFromFoodItem(foodItem: FoodItem, portion: String = "") {
        val mealItem = MealItem(
            name = foodItem.name,
            category = foodItem.category,
            portion = portion
        )
        addMealItem(mealItem)
        
        // Record usage for suggestions
        viewModelScope.launch {
            foodItemRepository.recordItemUsage(foodItem.id)
        }
    }

    fun addCustomMealItem(name: String, category: FoodCategory, portion: String = "") {
        val mealItem = MealItem(
            name = name,
            category = category,
            portion = portion
        )
        addMealItem(mealItem)
        
        // Add to food catalog for future suggestions
        viewModelScope.launch {
            foodItemRepository.addFoodItemIfNotExists(name, category)
        }
    }

    fun removeMealItem(item: MealItem) {
        _uiState.update { state ->
            state.copy(mealItems = state.mealItems - item)
        }
    }

    fun showAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = true, itemSearchQuery = "") }
    }

    fun dismissAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = false, itemSearchQuery = "") }
    }

    fun onItemSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(itemSearchQuery = query) }
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun showTimePicker() {
        _uiState.update { it.copy(showTimePicker = true) }
    }

    fun dismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false) }
    }

    fun save() {
        val state = _uiState.value
        
        // Validation
        if (state.mealType == null) {
            _uiState.update { it.copy(error = "Veuillez sélectionner un type de repas") }
            return
        }
        
        if (state.selectedChildIds.isEmpty()) {
            _uiState.update { it.copy(error = "Veuillez sélectionner au moins un enfant") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val meal = Meal(
                    id = mealId ?: 0,
                    date = state.date,
                    time = state.time,
                    mealType = state.mealType,
                    notes = state.notes.trim(),
                    items = state.mealItems
                )
                
                if (mealId != null) {
                    mealRepository.updateMeal(meal, state.selectedChildIds.toList(), state.mealItems)
                } else {
                    mealRepository.addMeal(meal, state.selectedChildIds.toList(), state.mealItems)
                }
                
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Échec de l'enregistrement du repas"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
