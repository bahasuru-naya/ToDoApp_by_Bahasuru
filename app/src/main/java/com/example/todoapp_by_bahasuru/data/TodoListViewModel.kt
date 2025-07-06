package com.example.todoapp_by_bahasuru.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TodoListViewModel(private val repository: TodoListRepository) : ViewModel() {

    // all list
    val allLists: LiveData<List<ListEntity>> = repository.allLists


    // selected list's items
    private var _currentListItems: LiveData<List<TodoItemEntity>>? = null

    val currentListItems: LiveData<List<TodoItemEntity>>
        get() = _currentListItems ?: throw IllegalStateException("No list selected yet.")


    //list
    fun selectList(listId: Long) {
        _currentListItems = repository.getTodoItemsForList(listId)
    }


    fun addList(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertList(ListEntity(name = name))
        }
    }
    fun updateList(list: ListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateList(list)
        }
    }

    fun deleteList(list: ListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteList(list)
        }
    }

    fun getListNameById(listId: Long, callback: (String) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                repository.getListNameById(listId)
            }
            callback(name)
        }
    }

    //items

    fun addTodoItem(listId: Long,title: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextIndex = repository.getNextOrderIndexForList(listId)
            repository.insertTodoItem(TodoItemEntity(listId = listId, title=title, description = description, orderIndex = nextIndex))
        }
    }

    fun updateTodoItem(item: TodoItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTodoItem(item)
        }
    }

    fun deleteTodoItem(item: TodoItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItemAndShiftOrder(item)
        }
    }

    fun fetchOrderIndex(itemId: Long, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val index = repository.getOrderIndex(itemId)
            onResult(index)
        }
    }

    fun swapWithNext(listId: Long, currentIndex: Int) {
        viewModelScope.launch {
            repository.swapOrderWithNext(listId, currentIndex)
        }
    }

    fun swapWithPrevious(listId: Long, currentIndex: Int) {
        viewModelScope.launch {
            repository.swapOrderWithPrevious(listId, currentIndex)
        }
    }

    //search
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    //search list
    val searchListResults: LiveData<List<ListEntity>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) MutableLiveData(emptyList())
        else repository.searchLists(query)
    }

    //get list index
    val reversedIndexList:  LiveData<List<Pair<Int, Long>>> = repository.getIndexMap()

    //search item
    val getItemResults: LiveData<List<TodoItemEntity>> = _searchQuery.switchMap { query ->
            if (query.isBlank()) MutableLiveData(emptyList())
            else repository.searchItemsInListByTitle( query)
    }

    //get stat
    fun getTotalCountLive(listId: Long): LiveData<Int> = repository.getTotalItemCount(listId)

    fun getCompletedCountLive(listId: Long): LiveData<Int> = repository.getCompletedItemCount(listId)



    //backup and restore
    // LiveData to track the state of backup/restore operations
    private val _operationStatus = MutableLiveData<Result<String>>()
    val operationStatus: LiveData<Result<String>> = _operationStatus

    // Function to trigger backup
    fun backupUserData(username: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.backupToFirestore(username)
                _operationStatus.postValue(Result.success("Backup successful!"))


            } catch (e: Exception) {
                _operationStatus.postValue(Result.failure(e))
            }
        }
    }

    // Function to trigger restore
    fun restoreUserData(username: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.restoreFromFirestore(username)
                _operationStatus.postValue(Result.success("Restore successful!"))
            } catch (e: Exception) {
                _operationStatus.postValue(Result.failure(e))
            }
        }
    }

}

// Factory for creating the ViewModel with the repository dependency
class TodoListViewModelFactory(private val repository: TodoListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}