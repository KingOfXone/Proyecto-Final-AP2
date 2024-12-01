package edu.ucne.taskmaster.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Label")
data class LabelEntity(
    @PrimaryKey
    val id: Int = 0,
    val description: String = "",
    val hexColor: String = "",
    val synchronized: Int = 0
)

