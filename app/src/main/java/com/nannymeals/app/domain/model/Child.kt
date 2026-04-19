package com.nannymeals.app.domain.model

import android.content.Context
import com.nannymeals.app.R
import java.time.LocalDate
import java.time.Period

data class Child(
    val id: Long = 0,
    val name: String,
    val dateOfBirth: LocalDate?,
    val dietaryRestrictions: String = "",
    val allergies: String = "",
    val parentEmail: String = "",
    val parentName: String = ""
) {
    val age: Int?
        get() = dateOfBirth?.let { 
            Period.between(it, LocalDate.now()).years 
        }
    
    fun getAgeDisplay(context: Context): String {
        return age?.let { 
            context.getString(R.string.age_years, it)
        } ?: context.getString(R.string.age_not_specified)
    }
    
    val hasAllergies: Boolean
        get() = allergies.isNotBlank()
    
    val hasDietaryRestrictions: Boolean
        get() = dietaryRestrictions.isNotBlank()
    
    val allergiesList: List<String>
        get() = allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    
    val dietaryRestrictionsList: List<String>
        get() = dietaryRestrictions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
