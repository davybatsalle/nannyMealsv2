package com.nannymeals.app.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.domain.repository.ChildRepository
import com.nannymeals.app.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MealsListUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val mealToDelete: Meal? = null,
    val showCalendar: Boolean = true
)

@HiltViewModel
class MealsListViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealsListUiState())
    val uiState: StateFlow<MealsListUiState> = _uiState.asStateFlow()

    private val _mealsForSelectedDate = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate: StateFlow<List<Meal>> = _mealsForSelectedDate.asStateFlow()

    private val _mealsForMonth = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForMonth: StateFlow<List<Meal>> = _mealsForMonth.asStateFlow()

    val children: StateFlow<List<Child>> = childRepository.getChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadMealsForSelectedDate()
        loadMealsForMonth()
    }

    private fun loadMealsForSelectedDate() {
        viewModelScope.launch {
            mealRepository.getMealsByDate(_uiState.value.selectedDate).collect { meals ->
                _mealsForSelectedDate.value = meals
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadMealsForMonth() {
        viewModelScope.launch {
            val month = _uiState.value.currentMonth
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            
            mealRepository.getMealsByDateRange(startDate, endDate).collect { meals ->
                _mealsForMonth.value = meals
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, isLoading = true) }
        loadMealsForSelectedDate()
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
        loadMealsForMonth()
    }

    fun previousMonth() {
        changeMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        changeMonth(_uiState.value.currentMonth.plusMonths(1))
    }

    fun goToToday() {
        val today = LocalDate.now()
        _uiState.update { 
            it.copy(
                selectedDate = today,
                currentMonth = YearMonth.from(today)
            ) 
        }
        loadMealsForSelectedDate()
        loadMealsForMonth()
    }

    fun toggleCalendarView() {
        _uiState.update { it.copy(showCalendar = !it.showCalendar) }
    }

    fun showDeleteConfirmation(meal: Meal) {
        _uiState.update { it.copy(mealToDelete = meal) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(mealToDelete = null) }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            try {
                mealRepository.deleteMeal(mealId)
                _uiState.update { it.copy(mealToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Échec de la suppression du repas",
                        mealToDelete = null
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getDatesWithMeals(): Set<LocalDate> {
        return _mealsForMonth.value.map { it.date }.toSet()
    }

    /**
     * Generates a shareable text message summarizing the day's meals for Facebook sharing.
     * Returns null if there are no meals to share.
     */
    fun generateShareText(): String? {
        val meals = _mealsForSelectedDate.value
        if (meals.isEmpty()) return null

        val selectedDate = _uiState.value.selectedDate
        val today = LocalDate.now()
        
        val dateIntro = when {
            selectedDate == today -> "Aujourd'hui"
            selectedDate == today.minusDays(1) -> "Hier"
            else -> "Le ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.FRENCH))}"
        }

        val builder = StringBuilder()
        builder.append("$dateIntro chez Nanny, on a mangé :\n\n")

        // Group meals by type and list items
        meals.sortedBy { it.time }.forEach { meal ->
            val items = meal.items
            if (items.isNotEmpty()) {
                builder.append("🍽️ ${meal.mealTypeDisplay} :\n")
                items.forEach { item ->
                    builder.append("  • ${item.name}")
                    if (item.portion.isNotBlank()) {
                        builder.append(" (${item.portion})")
                    }
                    builder.append("\n")
                }
                builder.append("\n")
            }
        }

        builder.append("#NannyMeals")

        return builder.toString().trim()
    }
}
