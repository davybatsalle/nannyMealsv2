package com.nannymeals.app.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String? = null
)

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
