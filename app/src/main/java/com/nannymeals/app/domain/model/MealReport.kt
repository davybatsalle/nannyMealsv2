package com.nannymeals.app.domain.model

import java.time.LocalDate

data class MealReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalMeals: Int,
    val mealTypeCounts: Map<MealType, Int>,
    val mealsPerDay: Map<LocalDate, Int>,
    val childMealCounts: Map<Child, Int>,
    val insights: List<String>,
    val recommendations: List<String>
) {
    val averageMealsPerDay: Double
        get() = if (mealsPerDay.isNotEmpty()) {
            totalMeals.toDouble() / mealsPerDay.size
        } else {
            0.0
        }
    
    val mostCommonMealType: MealType?
        get() = mealTypeCounts.maxByOrNull { it.value }?.key
    
    val leastCommonMealType: MealType?
        get() = mealTypeCounts.filter { it.value > 0 }.minByOrNull { it.value }?.key
}

enum class ReportPeriod {
    LAST_WEEK,
    LAST_MONTH,
    LAST_THREE_MONTHS,
    CUSTOM
}
