package com.example.todoapp_by_bahasuru.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ListDao {

    @Insert
    suspend fun insertList(list: ListEntity): Long // Returns the new row ID

    @Query("SELECT * FROM lists ORDER BY id DESC") // Display lists in the order they are added
    fun getAllLists(): LiveData<List<ListEntity>> // LiveData for observing changes

    @Query("SELECT name FROM lists WHERE id = :listId")
    suspend fun getListNameById(listId: Long): String

    @Delete
    suspend fun deleteList(list: ListEntity)

    @Update
    suspend fun updateList(list: ListEntity) // In case you want to rename a list

    //search

    @Query("SELECT * FROM lists WHERE LOWER(name) LIKE '%' || LOWER(:searchQuery) || '%' ORDER BY id DESC")
    fun searchListsByName(searchQuery: String): LiveData<List<ListEntity>>

    //get index
    @Query("SELECT id FROM lists ORDER BY id DESC")
    fun getAllIdsDesc(): LiveData<List<Long>>


    //backup
    @Query("SELECT * FROM lists")
    suspend fun getAllListsForBackup(): List<ListEntity>

    @Query("DELETE FROM lists")
    suspend fun clearAllLists()

}