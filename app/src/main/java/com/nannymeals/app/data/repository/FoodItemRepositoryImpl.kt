package com.nannymeals.app.data.repository

import android.content.Context
import com.nannymeals.app.data.dao.FoodItemDao
import com.nannymeals.app.data.entity.DefaultFoodItems
import com.nannymeals.app.data.entity.FoodCategory
import com.nannymeals.app.data.entity.FoodItemEntity
import com.nannymeals.app.data.mapper.toEntity
import com.nannymeals.app.data.mapper.toFoodItem
import com.nannymeals.app.domain.model.FoodItem
import com.nannymeals.app.domain.repository.FoodItemRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodItemRepositoryImpl @Inject constructor(
    private val foodItemDao: FoodItemDao,
    @ApplicationContext private val context: Context
) : FoodItemRepository {

    private val userId: String = "local_user"

    override fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getAllFoodItems(userId).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override fun getFoodItemsByCategory(category: FoodCategory): Flow<List<FoodItem>> {
        return foodItemDao.getFoodItemsByCategory(userId, category).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> {
        return foodItemDao.searchFoodItems(userId, query).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override fun getMostUsedItems(limit: Int): Flow<List<FoodItem>> {
        return foodItemDao.getMostUsedItems(userId, limit).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override fun getSuggestedItems(daysAgo: Int, limit: Int): Flow<List<FoodItem>> {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo.toLong())
        return foodItemDao.getSuggestedItems(userId, cutoffTime, limit).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override fun getUnusedItems(limit: Int): Flow<List<FoodItem>> {
        return foodItemDao.getUnusedItems(userId, limit).map { items ->
            items.map { it.toFoodItem() }
        }
    }

    override suspend fun addFoodItem(item: FoodItem): Long {
        return foodItemDao.insertFoodItem(item.toEntity(userId))
    }

    override suspend fun addFoodItemIfNotExists(name: String, category: FoodCategory): Long {
        val existing = foodItemDao.getFoodItemByName(userId, name)
        if (existing != null) {
            return existing.id
        }
        val newItem = FoodItemEntity(
            userId = userId,
            name = name,
            category = category,
            isDefault = false
        )
        return foodItemDao.insertFoodItem(newItem)
    }

    override suspend fun recordItemUsage(itemId: Long) {
        foodItemDao.recordUsage(itemId)
    }

    override suspend fun recordItemUsages(itemIds: List<Long>) {
        foodItemDao.recordUsages(itemIds)
    }

    override suspend fun initializeDefaultItems() {
        // Clear existing defaults to ensure we have the localized names
        foodItemDao.deleteDefaultFoodItems(userId)
        
        // Add default food items with current system language
        val defaultItems = DefaultFoodItems.getAllDefaults().map { (resId, category) ->
            FoodItemEntity(
                userId = userId,
                name = context.getString(resId),
                category = category,
                isDefault = true
            )
        }
        
        foodItemDao.insertFoodItems(defaultItems)
    }
}
