package edu.ucne.taskmaster.presentation.TaskList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucne.taskmaster.data.local.entities.LabelEntity
import edu.ucne.taskmaster.data.local.entities.TaskLabelEntity
import edu.ucne.taskmaster.repository.LabelRepository
import edu.ucne.taskmaster.repository.TaskLabelRepository
import edu.ucne.taskmaster.repository.TaskRepository
import edu.ucne.taskmaster.repository.TasksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject


@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val taskRepository: TaskRepository,
    private val labelRepository: LabelRepository,
    private val taskLabelRepository: TaskLabelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch { onEvent(TaskListUiEvent.GetTasks) }
            launch { onEvent(TaskListUiEvent.GetLabels) }
            launch { onEvent(TaskListUiEvent.GetLabelDescription) }
        }

        viewModelScope.launch {
            uiState.collect { state ->
                if (state.tasks.isNotEmpty() && state.currentOrder == 0) {
                    onEvent(TaskListUiEvent.OnOrderChange("Created Date"))
                }
            }
        }

    }

    fun onEvent(event: TaskListUiEvent) {
        when (event) {
            is TaskListUiEvent.CreateDateChange -> _uiState.update { it.copy(createdDate = event.createdDate) }
            is TaskListUiEvent.DescriptionChange -> _uiState.update { it.copy(description = event.description) }
            is TaskListUiEvent.DueDateChange -> _uiState.update { it.copy(dueDate = event.dueDate) }
            is TaskListUiEvent.PriorityChange -> _uiState.update { it.copy(priority = event.priority) }
            is TaskListUiEvent.TitleChange -> _uiState.update { it.copy(title = event.title) }
            is TaskListUiEvent.FilterLabelToggle -> {
                filterLabelToggle(event.labelsSelected)
            }

            is TaskListUiEvent.LabelToggle -> {
                labelToggle(event.labelsSelected)

            }

            is TaskListUiEvent.GetTask -> getTask(event.id)
            is TaskListUiEvent.SaveTask -> saveTask()
            is TaskListUiEvent.GetLabels -> getLabels()
            is TaskListUiEvent.DeleteTask -> deleteTask(event.id)
            is TaskListUiEvent.GetTasks -> getTasks()
            is TaskListUiEvent.OnOrderChange -> onOrderChange(event.order)
            is TaskListUiEvent.GetLabelDescription -> getLabelDescription()
            is TaskListUiEvent.SearchQueryChange -> {
                onSearchQueryChange(event.query)
            }

            is TaskListUiEvent.ResetFilters -> resetFilters()
            is TaskListUiEvent.ApplyFilters -> applyFilters()
            is TaskListUiEvent.Validate -> validate()
        }
    }


    private fun saveTask() {
        viewModelScope.launch {
            val task = _uiState.value.toEntity()
            val id = taskRepository.insertAndGetId(task)
            taskLabelRepository.deleteTaskLabelByTaskIdRoom(id.toInt())
            _uiState.value.labelsSelected.forEach { label ->
                taskLabelRepository.saveTaskLabelRoom(
                    TaskLabelEntity(
                        taskId = id.toInt(),
                        labelId = label.id!!,
                    )
                )
            }
        }
    }

    private fun getLabels() {
        viewModelScope.launch {
            val labels = labelRepository.getLabelsRoom()
            _uiState.update {
                it.copy(
                    labels = labels
                )
            }
        }
    }

    private fun deleteTask(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.deleteTaskRoom(id)
            taskLabelRepository.deleteTaskLabelByTaskIdRoom(id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun getTasks() {
        viewModelScope.launch {
            tasksRepository.getTasksWithRealLabels().let { tasks ->
                if (tasks.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se encontraron tareas"
                        )
                    }
                }
            }
        }
    }

    private fun getTask(id: Int) {
        viewModelScope.launch {
            tasksRepository.getTaskWithRealLabelsById(id).let { tasks ->
                val labels = _uiState.value.labels - tasks.labels.toSet()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = tasks.task.title ?: "",
                        description = tasks.task.description ?: "",
                        createdDate = tasks.task.createdDate,
                        dueDate = tasks.task.dueDate,
                        priority = tasks.task.priority,
                        labels = labels,
                        labelsSelected = tasks.labels,
                        id = tasks.task.taskId ?: 0,
                        error = null
                    )
                }
            }
        }
    }

    fun onDateClick() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(showModal = !_uiState.value.showModal)
            }
        }
    }

    fun changeSelectedDate(selectedDate: Date) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dueDate = selectedDate
                )
            }
        }
    }

    private fun onOrderChange(order: String) {
        val currentState = _uiState.value
        val newIndex = currentState.orderBy.indexOf(order)

        if (newIndex == -1) return

        val sortedTasks = when (order) {
            "Created Date" -> currentState.tasks.sortedByDescending { it.task.createdDate }
            "Due Date" -> currentState.tasks.sortedBy { it.task.dueDate }
            "Priority" -> currentState.tasks.sortedBy { it.task.priority }
            else -> currentState.tasks
        }

        _uiState.update {
            it.copy(
                currentOrder = newIndex,
                tasks = sortedTasks
            )
        }
    }

    private fun getLabelDescription() {
        viewModelScope.launch {
            val labelsDescription = labelRepository.getLabelDescription()
            _uiState.update { it.copy(labelsDescription = labelsDescription) }
        }
    }

    private fun filterLabelToggle(label: LabelEntity) {
        val currentLabels = _uiState.value.labelsSelected.toMutableList()
        if (currentLabels.contains(label)) {
            currentLabels.remove(label)
        } else {
            currentLabels.add(label)
        }
        _uiState.update { it.copy(labelsSelected = currentLabels) }
        applyFilters()
    }


    private fun applyFilters() {
        viewModelScope.launch {
            val query = _uiState.value.description
            val selectedLabels = _uiState.value.labelsSelected.map { it.id }

            val filteredTasks = tasksRepository.searchTasksWithRealLabels(query).filter { task ->
                selectedLabels.isEmpty() || task.labels.any { it.id in selectedLabels }
            }

            _uiState.update { it.copy(tasks = filteredTasks) }
        }
    }

    private fun resetFilters() {
        _uiState.update {
            it.copy(
                description = "",
                labelsSelected = emptyList()
            )
        }
        applyFilters()
    }

    private fun labelToggle(labelsSelected: LabelEntity) {
        if (_uiState.value.labelsSelected.contains(labelsSelected)) {
            _uiState.value = _uiState.value.copy(
                labelsSelected = _uiState.value.labelsSelected - labelsSelected,
                labels = _uiState.value.labels + labelsSelected
            )
        } else {
            _uiState.value = _uiState.value.copy(
                labelsSelected = _uiState.value.labelsSelected + labelsSelected,
                labels = _uiState.value.labels - labelsSelected
            )
        }
    }


    private fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(description = query) }
        applyFilters()
    }

    private fun validate() {
        val titleError =
            if (_uiState.value.title.isBlank()) "El título no puede estar vacío" else null
        val descriptionError =
            if (_uiState.value.description.isBlank()) "La descripción no puede estar vacía" else null

        _uiState.update {
            it.copy(
                titleError = titleError,
                descriptionError = descriptionError,
            )
        }
    }
}


