package com.nannymeals.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.nannymeals.app.domain.model.AuthState
import com.nannymeals.app.domain.model.User
import com.nannymeals.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    // Demo mode flag - set to true for local testing without Firebase
    private val demoMode = false
    
    // Demo user for testing
    private val demoUser = User(
        uid = "demo-user-001",
        email = "demo@nannymeals.app",
        displayName = "Demo Childminder"
    )
    
    private val _demoAuthState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override val authState: Flow<AuthState> = if (demoMode) {
        _demoAuthState.asStateFlow()
    } else {
        callbackFlow {
            trySend(AuthState.Loading)
            
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    trySend(AuthState.Authenticated(
                        User(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName
                        )
                    ))
                } else {
                    trySend(AuthState.Unauthenticated)
                }
            }
            
            firebaseAuth.addAuthStateListener(authStateListener)
            
            awaitClose {
                firebaseAuth.removeAuthStateListener(authStateListener)
            }
        }
    }

    override val currentUser: User?
        get() = if (demoMode) {
            if (_demoAuthState.value is AuthState.Authenticated) demoUser else null
        } else {
            firebaseAuth.currentUser?.let { firebaseUser ->
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName
                )
            }
        }

    override suspend fun signIn(email: String, password: String): Result<User> {
        if (demoMode) {
            // Accept any credentials in demo mode
            _demoAuthState.value = AuthState.Authenticated(demoUser)
            return Result.success(demoUser)
        }
        
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName
                    )
                )
            } else {
                Result.failure(Exception("Échec de la connexion"))
            }
        } catch (e: FirebaseAuthException) {
            Result.failure(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        if (demoMode) {
            // Accept any credentials in demo mode
            val newUser = demoUser.copy(email = email)
            _demoAuthState.value = AuthState.Authenticated(newUser)
            return Result.success(newUser)
        }
        
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName
                    )
                )
            } else {
                Result.failure(Exception("Échec de la création du compte"))
            }
        } catch (e: FirebaseAuthException) {
            Result.failure(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        if (demoMode) {
            _demoAuthState.value = AuthState.Unauthenticated
            return
        }
        firebaseAuth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (demoMode) {
            // Just return success in demo mode
            return Result.success(Unit)
        }
        
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Result.failure(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapFirebaseAuthException(e: FirebaseAuthException): Exception {
        val message = when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
            else -> e.message ?: "Authentication failed"
        }
        return Exception(message)
    }
}
