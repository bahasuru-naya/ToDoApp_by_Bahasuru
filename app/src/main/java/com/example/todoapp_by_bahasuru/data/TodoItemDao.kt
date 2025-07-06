package com.example.todoapp_by_bahasuru.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TodoItemDao {
    @Insert
    suspend fun insertTodoItem(item: TodoItemEntity)

    @Update
    suspend fun updateTodoItem(item: TodoItemEntity)

    @Delete
    suspend fun deleteTodoItem(item: TodoItemEntity)

    @Query("SELECT * FROM todo_items WHERE listId = :listId ORDER BY orderIndex ASC")
    fun getTodoItemsForList(listId: Long): LiveData<List<TodoItemEntity>>


    //maintain order index

    @Query("SELECT MAX(orderIndex) FROM todo_items WHERE listId = :listId")
    suspend fun getMaxOrderIndexForList(listId: Long): Int?

    @Query("""
    UPDATE todo_items
    SET orderIndex = orderIndex - 1     
    WHERE listId = :listId AND orderIndex > :deletedOrderIndex
    """)
    suspend fun shiftOrderIndicesAfterDeletion(listId: Long, deletedOrderIndex: Int)

    //get order index
    @Query("SELECT orderIndex FROM todo_items WHERE id = :itemId LIMIT 1")
    suspend fun getOrderIndexByItemId(itemId: Long): Int?

    //move item up down
    @Query("SELECT * FROM todo_items WHERE listId = :listId AND orderIndex = :orderIndex LIMIT 1")
    suspend fun getItemByOrderIndex(listId: Long, orderIndex: Int): TodoItemEntity?


    //search
    @Query("SELECT * FROM todo_items WHERE LOWER(title) LIKE '%' || LOWER(:searchQuery) || '%' ORDER BY id ASC")
    fun searchTodoItemsByTitle(searchQuery: String): LiveData<List<TodoItemEntity>>


    //get stat
    @Query("SELECT COUNT(*) FROM todo_items WHERE listId = :listId")
    fun getTotalItemCountForList(listId: Long): LiveData<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE listId = :listId AND completed = 1")
    fun getCompletedItemCountForList(listId: Long): LiveData<Int>


    //back up
    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodoItemsForBackup(): List<TodoItemEntity>



}