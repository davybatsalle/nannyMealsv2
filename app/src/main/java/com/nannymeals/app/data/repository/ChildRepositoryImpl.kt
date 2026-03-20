package com.nannymeals.app.data.repository

import com.nannymeals.app.data.dao.ChildDao
import com.nannymeals.app.data.mapper.toChild
import com.nannymeals.app.data.mapper.toEntity
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.repository.ChildRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val childDao: ChildDao
) : ChildRepository {

    private val userId: String = "local_user"

    override fun getChildren(): Flow<List<Child>> {
        return childDao.getChildrenByUser(userId).map { entities ->
            entities.map { it.toChild() }
        }
    }

    override fun getChildById(childId: Long): Flow<Child?> {
        return childDao.getChildByIdFlow(childId).map { it?.toChild() }
    }

    override suspend fun addChild(child: Child): Long {
        return childDao.insertChild(child.toEntity(userId))
    }

    override suspend fun updateChild(child: Child) {
        childDao.updateChild(child.toEntity(userId))
    }

    override suspend fun deleteChild(childId: Long) {
        childDao.deleteChildById(childId)
    }

    override suspend fun getChildCount(): Int {
        return childDao.getChildCount(userId)
    }
}
