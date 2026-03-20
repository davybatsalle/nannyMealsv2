package com.nannymeals.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a food item within a meal.
 * Each meal can have multiple food items (main dish, sides, drinks, etc.)
 */
@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val name: String,
    val category: FoodCategory,
    val portion: String = "", // e.g., "1 cup", "2 slices", etc.
    val notes: String = ""
)

/**
 * Categories for food items to help with organization and suggestions
 */
enum class FoodCategory {
    PROTEIN,      // Chicken, beef, fish, eggs, beans, etc.
    GRAIN,        // Rice, pasta, bread, cereals, etc.
    VEGETABLE,    // Carrots, broccoli, peas, etc.
    FRUIT,        // Apple, banana, berries, etc.
    DAIRY,        // Milk, cheese, yogurt, etc.
    DRINK,        // Water, juice, etc.
    SNACK,        // Crackers, cookies, etc.
    OTHER
}
