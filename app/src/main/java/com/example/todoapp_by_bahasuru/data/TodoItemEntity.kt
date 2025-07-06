package com.example.todoapp_by_bahasuru.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE // When a list is deleted, all its items are also deleted
        )
    ],
    indices = [Index(value = ["listId", "orderIndex"], unique = false)] // Index for efficient queries and ordering
)
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // Auto-generated unique ID for each item
    val listId: Long = 0L, // Foreign key linking to the parent ListEntity
    val title: String = "", // The title of the TO-DO item
    val description: String  = "", // The short text description of the TO-DO item
    val completed: Boolean = false, // Tracks the completion status of the item
    val orderIndex: Int = 0// Used to maintain and change the order of items within a list
)