package edu.ucne.taskmaster.presentation.Label

import edu.ucne.taskmaster.data.local.entities.LabelEntity

data class LabelUiState(
    val isLoading: Boolean = false,
    val id: Int = 0,
    val description: String = "",
    val hexColor: String = "",
    val labels: List<LabelEntity> = emptyList(),
    val error: String? = null
)

