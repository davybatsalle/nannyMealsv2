package com.nannymeals.app.ui.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannymeals.app.domain.model.Child
import com.nannymeals.app.domain.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildrenListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val childToDelete: Child? = null
)

@HiltViewModel
class ChildrenListViewModel @Inject constructor(
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildrenListUiState())
    val uiState: StateFlow<ChildrenListUiState> = _uiState.asStateFlow()

    val children: StateFlow<List<Child>> = childRepository.getChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            children.collect {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun showDeleteConfirmation(child: Child) {
        _uiState.update { it.copy(childToDelete = child) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(childToDelete = null) }
    }

    fun deleteChild(childId: Long) {
        viewModelScope.launch {
            try {
                childRepository.deleteChild(childId)
                _uiState.update { it.copy(childToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Échec de la suppression de l'enfant",
                        childToDelete = null
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
