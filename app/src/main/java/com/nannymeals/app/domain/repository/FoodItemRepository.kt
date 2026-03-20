package com.nannymeals.app.domain.repository

import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

interface FoodItemRepository {
    fun getAllFoodItems(): Flow<List<FoodItem>>
    fun getFoodItemsByCategory(category: FoodCategory): Flow<List<FoodItem>>
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    fun getMostUsedItems(limit: Int = 10): Flow<List<FoodItem>>
    
    /**
     * Get items not used recently - great for variety
     * @param daysAgo Number of days to look back
     */
    fun getSuggestedItems(daysAgo: Int = 7, limit: Int = 10): Flow<List<FoodItem>>
    
    /**
     * Get items that have never been used (try something new)
     */
    fun getUnusedItems(limit: Int = 10): Flow<List<FoodItem>>
    
    suspend fun addFoodItem(item: FoodItem): Long
    suspend fun addFoodItemIfNotExists(name: String, category: FoodCategory): Long
    suspend fun recordItemUsage(itemId: Long)
    suspend fun recordItemUsages(itemIds: List<Long>)
    suspend fun initializeDefaultItems()
}
