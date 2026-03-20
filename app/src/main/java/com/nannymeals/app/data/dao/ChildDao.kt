package com.nannymeals.app.data.dao

import androidx.room.*
import com.nannymeals.app.data.entity.ChildEntity
import com.nannymeals.app.data.entity.ChildWithMeals
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {

    @Query("SELECT * FROM children WHERE userId = :userId ORDER BY name ASC")
    fun getChildrenByUser(userId: String): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE id = :childId")
    suspend fun getChildById(childId: Long): ChildEntity?

    @Query("SELECT * FROM children WHERE id = :childId")
    fun getChildByIdFlow(childId: Long): Flow<ChildEntity?>

    @Transaction
    @Query("SELECT * FROM children WHERE id = :childId")
    suspend fun getChildWithMeals(childId: Long): ChildWithMeals?

    @Transaction
    @Query("SELECT * FROM children WHERE userId = :userId")
    fun getChildrenWithMeals(userId: String): Flow<List<ChildWithMeals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: ChildEntity): Long

    @Update
    suspend fun updateChild(child: ChildEntity)

    @Delete
    suspend fun deleteChild(child: ChildEntity)

    @Query("DELETE FROM children WHERE id = :childId")
    suspend fun deleteChildById(childId: Long)

    @Query("SELECT COUNT(*) FROM children WHERE userId = :userId")
    suspend fun getChildCount(userId: String): Int
}
