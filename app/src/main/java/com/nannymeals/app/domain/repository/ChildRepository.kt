package com.nannymeals.app.domain.repository

import com.nannymeals.app.domain.model.Child
import kotlinx.coroutines.flow.Flow

interface ChildRepository {
    fun getChildren(): Flow<List<Child>>
    fun getChildById(childId: Long): Flow<Child?>
    suspend fun addChild(child: Child): Long
    suspend fun updateChild(child: Child)
    suspend fun deleteChild(childId: Long)
    suspend fun getChildCount(): Int
}
