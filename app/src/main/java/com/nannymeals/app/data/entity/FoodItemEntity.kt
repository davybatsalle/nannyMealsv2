package com.nannymeals.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
        "Poulet" to FoodCategory.PROTEIN,
        "Bœuf" to FoodCategory.PROTEIN,
        "Poisson" to FoodCategory.PROTEIN,
        "Dinde" to FoodCategory.PROTEIN,
        "Porc" to FoodCategory.PROTEIN,
        "Œufs" to FoodCategory.PROTEIN,
        "Tofu" to FoodCategory.PROTEIN,
        "Haricots" to FoodCategory.PROTEIN,
        "Lentilles" to FoodCategory.PROTEIN,
        "Fromage" to FoodCategory.PROTEIN
    )
    
    val grains = listOf(
        "Riz" to FoodCategory.GRAIN,
        "Pâtes" to FoodCategory.GRAIN,
        "Pain" to FoodCategory.GRAIN,
        "Céréales" to FoodCategory.GRAIN,
        "Flocons d'avoine" to FoodCategory.GRAIN,
        "Crêpes" to FoodCategory.GRAIN,
        "Tartine" to FoodCategory.GRAIN,
        "Biscuits salés" to FoodCategory.GRAIN,
        "Tortilla" to FoodCategory.GRAIN,
        "Nouilles" to FoodCategory.GRAIN
    )
    
    val vegetables = listOf(
        "Carottes" to FoodCategory.VEGETABLE,
        "Brocoli" to FoodCategory.VEGETABLE,
        "Petits pois" to FoodCategory.VEGETABLE,
        "Haricots verts" to FoodCategory.VEGETABLE,
        "Maïs" to FoodCategory.VEGETABLE,
        "Patate douce" to FoodCategory.VEGETABLE,
        "Pomme de terre" to FoodCategory.VEGETABLE,
        "Tomate" to FoodCategory.VEGETABLE,
        "Concombre" to FoodCategory.VEGETABLE,
        "Épinards" to FoodCategory.VEGETABLE
    )
    
    val fruits = listOf(
        "Pomme" to FoodCategory.FRUIT,
        "Banane" to FoodCategory.FRUIT,
        "Orange" to FoodCategory.FRUIT,
        "Raisins" to FoodCategory.FRUIT,
        "Fraises" to FoodCategory.FRUIT,
        "Myrtilles" to FoodCategory.FRUIT,
        "Pastèque" to FoodCategory.FRUIT,
        "Poire" to FoodCategory.FRUIT,
        "Pêche" to FoodCategory.FRUIT,
        "Mangue" to FoodCategory.FRUIT
    )
    
    val dairy = listOf(
        "Lait" to FoodCategory.DAIRY,
        "Yaourt" to FoodCategory.DAIRY,
        "Fromage" to FoodCategory.DAIRY,
        "Fromage blanc" to FoodCategory.DAIRY
    )
    
    val drinks = listOf(
        "Eau" to FoodCategory.DRINK,
        "Jus de fruits" to FoodCategory.DRINK,
        "Lait" to FoodCategory.DRINK,
        "Smoothie" to FoodCategory.DRINK
    )
    
    val snacks = listOf(
        "Biscuits salés" to FoodCategory.SNACK,
        "Compote de fruits" to FoodCategory.SNACK,
        "Barre de céréales" to FoodCategory.SNACK,
        "Biscuits" to FoodCategory.SNACK,
        "Bretzels" to FoodCategory.SNACK,
        "Bâtonnet de fromage" to FoodCategory.SNACK
    )
    
    fun getAllDefaults(): List<Pair<String, FoodCategory>> {
        return proteins + grains + vegetables + fruits + dairy + drinks + snacks
    }
}
