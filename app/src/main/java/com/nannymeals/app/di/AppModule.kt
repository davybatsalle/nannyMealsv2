package com.nannymeals.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth
import com.nannymeals.app.data.dao.ChildDao
import com.nannymeals.app.data.dao.FoodItemDao
import com.nannymeals.app.data.dao.MealDao
import com.nannymeals.app.data.database.NannyMealsDatabase
import com.nannymeals.app.data.repository.AuthRepositoryImpl
import com.nannymeals.app.data.repository.ChildRepositoryImpl
import com.nannymeals.app.data.repository.FoodItemRepositoryImpl
import com.nannymeals.app.data.repository.MealRepositoryImpl
import com.nannymeals.app.domain.repository.AuthRepository
import com.nannymeals.app.domain.repository.ChildRepository
import com.nannymeals.app.domain.repository.FoodItemRepository
import com.nannymeals.app.domain.repository.MealRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Migration from v1 to v2 - adds meal_items and food_items tables
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create meal_items table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meal_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    mealId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    portion TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY (mealId) REFERENCES meals(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_mealId ON meal_items (mealId)")
            
            // Create food_items table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS food_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    lastUsedDate INTEGER,
                    createdAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_food_items_userId_name ON food_items (userId, name)")
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NannyMealsDatabase {
        return Room.databaseBuilder(
            context,
            NannyMealsDatabase::class.java,
            NannyMealsDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration() // For testing - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun provideChildDao(database: NannyMealsDatabase): ChildDao {
        return database.childDao()
    }

    @Provides
    @Singleton
    fun provideMealDao(database: NannyMealsDatabase): MealDao {
        return database.mealDao()
    }

    @Provides
    @Singleton
    fun provideFoodItemDao(database: NannyMealsDatabase): FoodItemDao {
        return database.foodItemDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideChildRepository(
        childDao: ChildDao
    ): ChildRepository {
        return ChildRepositoryImpl(childDao)
    }

    @Provides
    @Singleton
    fun provideMealRepository(
        mealDao: MealDao,
        childDao: ChildDao
    ): MealRepository {
        return MealRepositoryImpl(mealDao, childDao)
    }

    @Provides
    @Singleton
    fun provideFoodItemRepository(
        foodItemDao: FoodItemDao,
        @ApplicationContext context: Context
    ): FoodItemRepository {
        return FoodItemRepositoryImpl(foodItemDao, context)
    }
}
