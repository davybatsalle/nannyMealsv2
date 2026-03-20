package com.nannymeals.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val dateOfBirth: LocalDate?,
    val dietaryRestrictions: String = "",
    val allergies: String = "",
    val parentEmail: String = "",
    val parentName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
