package com.nannymeals.app.domain.repository

import com.nannymeals.app.domain.model.AuthState
import com.nannymeals.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    val currentUser: User?
    
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
