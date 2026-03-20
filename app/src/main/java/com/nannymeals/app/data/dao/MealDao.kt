package com.nannymeals.app.data.dao

import androidx.room.*
import com.nannymeals.app.data.entity.MealChildCrossRef
import com.nannymeals.app.data.entity.MealEntity
import com.nannymeals.app.data.entity.MealItemEntity
import com.nannymeals.app.data.entity.MealType
import com.nannymeals.app.data.entity.MealWithChildren
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealDao {

    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getMealsByUser(userId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date ORDER BY time ASC")
    fun getMealsByDate(userId: String, date: LocalDate): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getMealsByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealById(mealId: Long): MealEntity?

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithChildren(mealId: Long): MealWithChildren?

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY date DESC, time DESC")
    fun getMealsWithChildren(userId: String): Flow<List<MealWithChildren>>

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date ORDER BY time ASC")
    fun getMealsWithChildrenByDate(userId: String, date: LocalDate): Flow<List<MealWithChildren>>

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getMealsWithChildrenByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<MealWithChildren>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealChildCrossRef(crossRef: MealChildCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealChildCrossRefs(crossRefs: List<MealChildCrossRef>)

    @Query("DELETE FROM meal_children WHERE mealId = :mealId")
    suspend fun deleteMealChildCrossRefs(mealId: Long)

    @Transaction
    suspend fun insertMealWithChildren(meal: MealEntity, childIds: List<Long>): Long {
        val mealId = insertMeal(meal)
        val crossRefs = childIds.map { childId ->
            MealChildCrossRef(mealId = mealId, childId = childId)
        }
        insertMealChildCrossRefs(crossRefs)
        return mealId
    }

    @Transaction
    suspend fun updateMealWithChildren(meal: MealEntity, childIds: List<Long>) {
        updateMeal(meal)
        deleteMealChildCrossRefs(meal.id)
        val crossRefs = childIds.map { childId ->
            MealChildCrossRef(mealId = meal.id, childId = childId)
        }
        insertMealChildCrossRefs(crossRefs)
    }

    // Meal Items operations
    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY category, name")
    suspend fun getMealItems(mealId: Long): List<MealItemEntity>

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY category, name")
    fun getMealItemsFlow(mealId: Long): Flow<List<MealItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItem(item: MealItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Update
    suspend fun updateMealItem(item: MealItemEntity)

    @Delete
    suspend fun deleteMealItem(item: MealItemEntity)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    suspend fun deleteMealItemsByMealId(mealId: Long)

    @Transaction
    suspend fun insertMealWithChildrenAndItems(
        meal: MealEntity, 
        childIds: List<Long>,
        items: List<MealItemEntity>
    ): Long {
        val mealId = insertMeal(meal)
        val crossRefs = childIds.map { childId ->
            MealChildCrossRef(mealId = mealId, childId = childId)
        }
        insertMealChildCrossRefs(crossRefs)
        
        // Insert items with the new meal ID
        val itemsWithMealId = items.map { it.copy(mealId = mealId) }
        insertMealItems(itemsWithMealId)
        
        return mealId
    }

    @Transaction
    suspend fun updateMealWithChildrenAndItems(
        meal: MealEntity, 
        childIds: List<Long>,
        items: List<MealItemEntity>
    ) {
        updateMeal(meal)
        deleteMealChildCrossRefs(meal.id)
        val crossRefs = childIds.map { childId ->
            MealChildCrossRef(mealId = meal.id, childId = childId)
        }
        insertMealChildCrossRefs(crossRefs)
        
        // Replace all items
        deleteMealItemsByMealId(meal.id)
        val itemsWithMealId = items.map { it.copy(mealId = meal.id) }
        insertMealItems(itemsWithMealId)
    }

    // Report queries
    @Query("SELECT mealType, COUNT(*) as count FROM meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate GROUP BY mealType")
    suspend fun getMealTypeCount(userId: String, startDate: LocalDate, endDate: LocalDate): List<MealTypeCount>

    @Query("SELECT COUNT(*) FROM meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalMealCount(userId: String, startDate: LocalDate, endDate: LocalDate): Int

    @Query("SELECT date, COUNT(*) as count FROM meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    suspend fun getMealsPerDay(userId: String, startDate: LocalDate, endDate: LocalDate): List<DailyMealCount>
}

data class MealTypeCount(
    val mealType: MealType,
    val count: Int
)

data class DailyMealCount(
    val date: LocalDate,
    val count: Int
)
