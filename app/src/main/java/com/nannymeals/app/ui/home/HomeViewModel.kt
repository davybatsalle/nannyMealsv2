package com.nannymeals.app.ui.home

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
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val children: StateFlow<List<Child>> = childRepository.getChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _todaysMeals = MutableStateFlow<List<Meal>>(emptyList())
    val todaysMeals: StateFlow<List<Meal>> = _todaysMeals.asStateFlow()

    private val _recentMeals = MutableStateFlow<List<Meal>>(emptyList())
    val recentMeals: StateFlow<List<Meal>> = _recentMeals.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            mealRepository.getMealsByDate(LocalDate.now()).collect { meals ->
                _todaysMeals.value = meals
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            val weekAgo = LocalDate.now().minusDays(7)
            mealRepository.getMealsByDateRange(weekAgo, LocalDate.now()).collect { meals ->
                _recentMeals.value = meals.take(10)
            }
        }
    }
}
