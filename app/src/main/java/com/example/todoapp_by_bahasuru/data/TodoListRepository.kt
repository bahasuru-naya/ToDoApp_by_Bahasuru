package com.example.todoapp_by_bahasuru.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class TodoListRepository(private val listDao: ListDao, private val todoItemDao: TodoItemDao) {

    //lists

    val allLists: LiveData<List<ListEntity>> = listDao.getAllLists()

    suspend fun insertList(list: ListEntity): Long {
        return listDao.insertList(list)
    }
    suspend fun updateList(list: ListEntity) {
        listDao.updateList(list)
    }

    suspend fun deleteList(list: ListEntity) {
        listDao.deleteList(list)
    }

    suspend fun getListNameById(listId: Long): String {
        return listDao.getListNameById(listId)
    }

    //items

    fun getTodoItemsForList(listId: Long): LiveData<List<TodoItemEntity>> {
        return todoItemDao.getTodoItemsForList(listId)
    }

    suspend fun insertTodoItem(item: TodoItemEntity) {
        todoItemDao.insertTodoItem(item)
    }

    // Get the next order index for a item
    suspend fun getNextOrderIndexForList(listId: Long): Int {
        return (todoItemDao.getMaxOrderIndexForList(listId) ?: -1) + 1
    }

    suspend fun updateTodoItem(item: TodoItemEntity) {
        todoItemDao.updateTodoItem(item)
    }

    suspend fun deleteItemAndShiftOrder(item: TodoItemEntity) {
        todoItemDao.deleteTodoItem(item)
        todoItemDao.shiftOrderIndicesAfterDeletion(item.listId, item.orderIndex)
    }

    suspend fun getOrderIndex(itemId: Long): Int? {
        return todoItemDao.getOrderIndexByItemId(itemId)
    }

    suspend fun swapOrderWithNext(listId: Long, currentIndex: Int) {
        val currentItem = todoItemDao.getItemByOrderIndex(listId, currentIndex)
        val nextItem = todoItemDao.getItemByOrderIndex(listId, currentIndex + 1)

        if (currentItem != null && nextItem != null) {
            val tempIndex = currentItem.orderIndex
            currentItem.copy(orderIndex = nextItem.orderIndex).also { todoItemDao.updateTodoItem(it) }
            nextItem.copy(orderIndex = tempIndex).also { todoItemDao.updateTodoItem(it) }
        }
    }

    suspend fun swapOrderWithPrevious(listId: Long, currentIndex: Int) {
        val currentItem = todoItemDao.getItemByOrderIndex(listId, currentIndex)
        val previousItem = todoItemDao.getItemByOrderIndex(listId, currentIndex - 1)

        if (currentItem != null && previousItem != null) {
            val tempIndex = currentItem.orderIndex
            todoItemDao.updateTodoItem(currentItem.copy(orderIndex = previousItem.orderIndex))
            todoItemDao.updateTodoItem(previousItem.copy(orderIndex = tempIndex))
        }
    }


    //search lists
    fun searchLists(query: String): LiveData<List<ListEntity>> {
        return listDao.searchListsByName(query)
    }

    fun getIndexMap():LiveData<List<Pair<Int, Long>>> {
        return listDao.getAllIdsDesc().map { idList ->
            idList.mapIndexed { index, id -> index to id }
        }
    }

    //search items
    fun searchItemsInListByTitle(query: String): LiveData<List<TodoItemEntity>> {
        return todoItemDao.searchTodoItemsByTitle(query)
    }

    //get stat
    fun getTotalItemCount(listId: Long): LiveData<Int> {
        return todoItemDao.getTotalItemCountForList(listId)
    }

    fun getCompletedItemCount(listId: Long): LiveData<Int> {
        return todoItemDao.getCompletedItemCountForList(listId)
    }

    //backup

    suspend fun backupToFirestore(username: String?) {
        val userId = username?: return // Ensure user is logged in

        val firestore = Firebase.firestore
        val userDocRef = firestore.collection("users").document(userId)

        // Get all data from Room
        val lists = listDao.getAllListsForBackup()
        val todoItems = todoItemDao.getAllTodoItemsForBackup()

        // Use a batch write for atomicity (all or nothing)
        firestore.runBatch { batch ->
            // Backup lists
            val listsCollection = userDocRef.collection("lists")
            lists.forEach { list ->
                // Use the Room ID as the document ID in Firestore
                val docRef = listsCollection.document(list.id.toString())
                batch.set(docRef, list)
            }

            // Backup todo items
            val itemsCollection = userDocRef.collection("todo_items")
            todoItems.forEach { item ->
                // Use the Room ID as the document ID in Firestore
                val docRef = itemsCollection.document(item.id.toString())
                batch.set(docRef, item)
            }
        }.await() // Coroutine waits for the batch write to complete
    }

    // restore all data from Firestore
    suspend fun restoreFromFirestore(username: String?) {
        val userId = username ?: return // Ensure user is logged in

        val firestore = Firebase.firestore
        val userDocRef = firestore.collection("users").document(userId)

        // Fetch data from Firestore
        val listsSnapshot = userDocRef.collection("lists").get().await()
        val itemsSnapshot = userDocRef.collection("todo_items").get().await()

        val lists = listsSnapshot.toObjects(ListEntity::class.java)
        val todoItems = itemsSnapshot.toObjects(TodoItemEntity::class.java)

        // Clear local database before inserting new data
        listDao.clearAllLists()

        // Insert restored data into Room
        lists.forEach { listDao.insertList(it) }
        todoItems.forEach { todoItemDao.insertTodoItem(it) }
    }

}