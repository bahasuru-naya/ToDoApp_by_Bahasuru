package com.example.todoapp_by_bahasuru.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // Auto-generated unique ID for each list
    val name: String = "" // The name/heading of the TO-DO list
)