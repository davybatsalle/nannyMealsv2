package com.nannymeals.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.nannymeals.app.R

/**
 * A catalog of food items that can be reused across meals.
 * This helps with suggestions and avoiding repetition.
 */
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["userId", "name"], unique = true)]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val category: FoodCategory,
    val isDefault: Boolean = false, // Pre-populated items
    val usageCount: Int = 0,        // Track how often this item is used
    val lastUsedDate: Long? = null, // Track when last used for variety suggestions
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Default food items to populate the catalog
 */
object DefaultFoodItems {
    
    val proteins = listOf(
        R.string.food_chicken to FoodCategory.PROTEIN,
        R.string.food_beef to FoodCategory.PROTEIN,
        R.string.food_fish to FoodCategory.PROTEIN,
        R.string.food_turkey to FoodCategory.PROTEIN,
        R.string.food_pork to FoodCategory.PROTEIN,
        R.string.food_eggs to FoodCategory.PROTEIN,
        R.string.food_tofu to FoodCategory.PROTEIN,
        R.string.food_beans to FoodCategory.PROTEIN,
        R.string.food_lentils to FoodCategory.PROTEIN,
        R.string.food_cheese to FoodCategory.PROTEIN
    )
    
    val grains = listOf(
        R.string.food_rice to FoodCategory.GRAIN,
        R.string.food_pasta to FoodCategory.GRAIN,
        R.string.food_bread to FoodCategory.GRAIN,
        R.string.food_cereal to FoodCategory.GRAIN,
        R.string.food_oatmeal to FoodCategory.GRAIN,
        R.string.food_pancakes to FoodCategory.GRAIN,
        R.string.food_toast to FoodCategory.GRAIN,
        R.string.food_crackers to FoodCategory.GRAIN,
        R.string.food_tortilla to FoodCategory.GRAIN,
        R.string.food_noodles to FoodCategory.GRAIN
    )
    
    val vegetables = listOf(
        R.string.food_carrots to FoodCategory.VEGETABLE,
        R.string.food_broccoli to FoodCategory.VEGETABLE,
        R.string.food_peas to FoodCategory.VEGETABLE,
        R.string.food_green_beans to FoodCategory.VEGETABLE,
        R.string.food_corn to FoodCategory.VEGETABLE,
        R.string.food_sweet_potato to FoodCategory.VEGETABLE,
        R.string.food_potato to FoodCategory.VEGETABLE,
        R.string.food_tomato to FoodCategory.VEGETABLE,
        R.string.food_cucumber to FoodCategory.VEGETABLE,
        R.string.food_spinach to FoodCategory.VEGETABLE
    )
    
    val fruits = listOf(
        R.string.food_apple to FoodCategory.FRUIT,
        R.string.food_banana to FoodCategory.FRUIT,
        R.string.food_orange to FoodCategory.FRUIT,
        R.string.food_grapes to FoodCategory.FRUIT,
        R.string.food_strawberries to FoodCategory.FRUIT,
        R.string.food_blueberries to FoodCategory.FRUIT,
        R.string.food_watermelon to FoodCategory.FRUIT,
        R.string.food_pear to FoodCategory.FRUIT,
        R.string.food_peach to FoodCategory.FRUIT,
        R.string.food_mango to FoodCategory.FRUIT
    )
    
    val dairy = listOf(
        R.string.food_milk to FoodCategory.DAIRY,
        R.string.food_yogurt to FoodCategory.DAIRY,
        R.string.food_cheese to FoodCategory.DAIRY,
        R.string.food_cottage_cheese to FoodCategory.DAIRY
    )
    
    val drinks = listOf(
        R.string.food_water to FoodCategory.DRINK,
        R.string.food_fruit_juice to FoodCategory.DRINK,
        R.string.food_milk to FoodCategory.DRINK,
        R.string.food_smoothie to FoodCategory.DRINK
    )
    
    val snacks = listOf(
        R.string.food_crackers to FoodCategory.SNACK,
        R.string.food_applesauce to FoodCategory.SNACK,
        R.string.food_granola_bar to FoodCategory.SNACK,
        R.string.food_cookies to FoodCategory.SNACK,
        R.string.food_pretzels to FoodCategory.SNACK,
        R.string.food_cheese_stick to FoodCategory.SNACK
    )
    
    fun getAllDefaults(): List<Pair<Int, FoodCategory>> {
        return proteins + grains + vegetables + fruits + dairy + drinks + snacks
    }
}
