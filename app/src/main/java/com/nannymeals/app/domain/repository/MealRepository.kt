package com.nannymeals.app.domain.repository

import com.nannymeals.app.domain.model.Meal
import com.nannymeals.app.domain.model.MealItem
import com.nannymeals.app.domain.model.MealReport
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MealRepository {
    fun getMeals(): Flow<List<Meal>>
    fun getMealsByDate(date: LocalDate): Flow<List<Meal>>
    fun getMealsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Meal>>
    suspend fun getMealById(mealId: Long): Meal?
    suspend fun addMeal(meal: Meal, childIds: List<Long>, items: List<MealItem> = emptyList()): Long
    suspend fun updateMeal(meal: Meal, childIds: List<Long>, items: List<MealItem> = emptyList())
    suspend fun deleteMeal(mealId: Long)
    suspend fun generateReport(startDate: LocalDate, endDate: LocalDate): MealReport
}
