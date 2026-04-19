package com.nannymeals.app.domain.model

import android.content.Context
import com.nannymeals.app.R
import com.nannymeals.app.data.entity.FoodCategory

/**
 * Domain model for a food item within a meal
 */
data class MealItem(
    val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val portion: String = "",
    val notes: String = ""
) {
    fun getCategoryDisplay(context: Context): String {
        val resId = when (category) {
            FoodCategory.PROTEIN -> R.string.category_protein
            FoodCategory.GRAIN -> R.string.category_grain
            FoodCategory.VEGETABLE -> R.string.category_vegetable
            FoodCategory.FRUIT -> R.string.category_fruit
            FoodCategory.DAIRY -> R.string.category_dairy
            FoodCategory.DRINK -> R.string.category_drink
            FoodCategory.SNACK -> R.string.category_snack
            FoodCategory.OTHER -> R.string.category_other
        }
        return context.getString(resId)
    }
}

/**
 * Domain model for a food item in the catalog (for suggestions)
 */
data class FoodItem(
    val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val usageCount: Int = 0,
    val lastUsedDate: Long? = null
) {
    fun getCategoryDisplay(context: Context): String {
        val resId = when (category) {
            FoodCategory.PROTEIN -> R.string.category_protein
            FoodCategory.GRAIN -> R.string.category_grain
            FoodCategory.VEGETABLE -> R.string.category_vegetable
            FoodCategory.FRUIT -> R.string.category_fruit
            FoodCategory.DAIRY -> R.string.category_dairy
            FoodCategory.DRINK -> R.string.category_drink
            FoodCategory.SNACK -> R.string.category_snack
            FoodCategory.OTHER -> R.string.category_other
        }
        return context.getString(resId)
    }
}
