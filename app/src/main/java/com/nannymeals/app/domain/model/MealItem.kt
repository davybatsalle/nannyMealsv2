package com.nannymeals.app.domain.model

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
    val categoryDisplay: String
        get() = when (category) {
            FoodCategory.PROTEIN -> "Protéine"
            FoodCategory.GRAIN -> "Céréale/Féculent"
            FoodCategory.VEGETABLE -> "Légume"
            FoodCategory.FRUIT -> "Fruit"
            FoodCategory.DAIRY -> "Produit laitier"
            FoodCategory.DRINK -> "Boisson"
            FoodCategory.SNACK -> "Collation"
            FoodCategory.OTHER -> "Autre"
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
    val categoryDisplay: String
        get() = when (category) {
            FoodCategory.PROTEIN -> "Protéine"
            FoodCategory.GRAIN -> "Céréale/Féculent"
            FoodCategory.VEGETABLE -> "Légume"
            FoodCategory.FRUIT -> "Fruit"
            FoodCategory.DAIRY -> "Produit laitier"
            FoodCategory.DRINK -> "Boisson"
            FoodCategory.SNACK -> "Collation"
            FoodCategory.OTHER -> "Autre"
        }
}
