package com.nannymeals.app.ui.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ChildFormUiState(
    val childId: Long? = null,
    val name: String = "",
    val dateOfBirth: LocalDate? = null,
    val dietaryRestrictions: String = "",
    val allergies: String = "",
    val parentName: String = "",
    val parentEmail: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val showDatePicker: Boolean = false
)

@HiltViewModel
class ChildFormViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val childId: Long? = savedStateHandle.get<Long>("childId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ChildFormUiState(childId = childId))
    val uiState: StateFlow<ChildFormUiState> = _uiState.asStateFlow()

    val isEditing: Boolean = childId != null

    init {
        childId?.let { loadChild(it) }
    }

    private fun loadChild(childId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            childRepository.getChildById(childId).collect { child ->
                child?.let {
                    _uiState.update { state ->
                        state.copy(
                            name = child.name,
                            dateOfBirth = child.dateOfBirth,
                            dietaryRestrictions = child.dietaryRestrictions,
                            allergies = child.allergies,
                            parentName = child.parentName,
                            parentEmail = child.parentEmail,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onDateOfBirthChange(date: LocalDate?) {
        _uiState.update { it.copy(dateOfBirth = date, error = null, showDatePicker = false) }
    }

    fun onDietaryRestrictionsChange(restrictions: String) {
        _uiState.update { it.copy(dietaryRestrictions = restrictions) }
    }

    fun onAllergiesChange(allergies: String) {
        _uiState.update { it.copy(allergies = allergies) }
    }

    fun onParentNameChange(name: String) {
        _uiState.update { it.copy(parentName = name) }
    }

    fun onParentEmailChange(email: String) {
        _uiState.update { it.copy(parentEmail = email) }
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun save() {
        val state = _uiState.value
        
        // Validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Le nom de l'enfant est requis") }
            return
        }
        
        if (state.parentEmail.isNotBlank() && 
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.parentEmail).matches()) {
            _uiState.update { it.copy(error = "Format d'e-mail du parent invalide") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val child = Child(
                    id = childId ?: 0,
                    name = state.name.trim(),
                    dateOfBirth = state.dateOfBirth,
                    dietaryRestrictions = state.dietaryRestrictions.trim(),
                    allergies = state.allergies.trim(),
                    parentName = state.parentName.trim(),
                    parentEmail = state.parentEmail.trim()
                )
                
                if (childId != null) {
                    childRepository.updateChild(child)
                } else {
                    childRepository.addChild(child)
                }
                
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Échec de l'enregistrement de l'enfant"
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
