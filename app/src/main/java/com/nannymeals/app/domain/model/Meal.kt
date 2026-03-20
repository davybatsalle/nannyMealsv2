package com.nannymeals.app.domain.model

import com.nannymeals.app.data.entity.MealType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Meal(
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val mealType: MealType,
    val notes: String = "",
    val children: List<Child> = emptyList(),
    val items: List<MealItem> = emptyList()
) {
    val formattedDate: String
        get() = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    
    val formattedTime: String
        get() = time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    
    val childrenNames: String
        get() = children.joinToString(", ") { it.name }
    
    val itemsSummary: String
        get() = items.joinToString(", ") { it.name }
    
    val mealTypeDisplay: String
        get() = when (mealType) {
            MealType.LUNCH -> "Déjeuner"
            MealType.SNACK -> "Collation"
        }
}
