package com.nannymeals.app.data.dao

import androidx.room.*
import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.data.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items WHERE userId = :userId ORDER BY usageCount DESC, name ASC")
    fun getAllFoodItems(userId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE userId = :userId AND category = :category ORDER BY usageCount DESC, name ASC")
    fun getFoodItemsByCategory(userId: String, category: FoodCategory): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY usageCount DESC, name ASC LIMIT 20")
    fun searchFoodItems(userId: String, query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE userId = :userId ORDER BY usageCount DESC LIMIT :limit")
    fun getMostUsedItems(userId: String, limit: Int = 10): Flow<List<FoodItemEntity>>

    /**
     * Get items not used in the last N days - great for variety suggestions
     */
    @Query("""
        SELECT * FROM food_items 
        WHERE userId = :userId 
        AND (lastUsedDate IS NULL OR lastUsedDate < :cutoffTime)
        AND usageCount > 0
        ORDER BY lastUsedDate ASC, usageCount DESC 
        LIMIT :limit
    """)
    fun getSuggestedItems(userId: String, cutoffTime: Long, limit: Int = 10): Flow<List<FoodItemEntity>>

    /**
     * Get items that have never been used (new suggestions)
     */
    @Query("""
        SELECT * FROM food_items 
        WHERE userId = :userId 
        AND usageCount = 0
        ORDER BY name ASC 
        LIMIT :limit
    """)
    fun getUnusedItems(userId: String, limit: Int = 10): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: Long): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getFoodItemByName(userId: String, name: String): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodItem(item: FoodItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Update
    suspend fun updateFoodItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(item: FoodItemEntity)

    /**
     * Increment usage count and update last used date
     */
    @Query("""
        UPDATE food_items 
        SET usageCount = usageCount + 1, lastUsedDate = :usedDate 
        WHERE id = :itemId
    """)
    suspend fun recordUsage(itemId: Long, usedDate: Long = System.currentTimeMillis())

    /**
     * Record usage for multiple items at once
     */
    @Transaction
    suspend fun recordUsages(itemIds: List<Long>) {
        val now = System.currentTimeMillis()
        itemIds.forEach { recordUsage(it, now) }
    }

    @Query("SELECT COUNT(*) FROM food_items WHERE userId = :userId")
    suspend fun getFoodItemCount(userId: String): Int
}
