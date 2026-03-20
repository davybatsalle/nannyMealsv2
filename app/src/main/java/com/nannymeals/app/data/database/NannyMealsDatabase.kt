package com.nannymeals.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nannymeals.app.data.dao.ChildDao
import com.nannymeals.app.data.dao.FoodItemDao
import com.nannymeals.app.data.dao.MealDao
import com.nannymeals.app.data.entity.ChildEntity
import com.nannymeals.app.data.entity.FoodItemEntity
import com.nannymeals.app.data.entity.MealChildCrossRef
import com.nannymeals.app.data.entity.MealEntity
import com.nannymeals.app.data.entity.MealItemEntity

@Database(
    entities = [
        ChildEntity::class,
        MealEntity::class,
        MealChildCrossRef::class,
        MealItemEntity::class,
        FoodItemEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NannyMealsDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun mealDao(): MealDao
    abstract fun foodItemDao(): FoodItemDao

    companion object {
        const val DATABASE_NAME = "nannymeals_database"
    }
}
