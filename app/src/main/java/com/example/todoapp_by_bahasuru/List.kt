package com.example.todoapp_by_bahasuru

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.todoapp_by_bahasuru.data.AppDatabase
import com.example.todoapp_by_bahasuru.data.ListEntity
import com.example.todoapp_by_bahasuru.data.TodoListRepository
import com.example.todoapp_by_bahasuru.data.TodoListViewModel
import com.example.todoapp_by_bahasuru.data.TodoListViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//--lists page--
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListAppBar(navController: NavController, initialScrollIndex: Int = 0) {
    val openDialogForEdit = remember { mutableStateOf(false) }
    val openDialogForDelete = remember { mutableStateOf(false) }
    val listName  = remember { mutableStateOf("") }
    val listId  = remember { mutableLongStateOf(0L) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))



    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = {
                    Text(
                        "All Lists",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home_screen") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("search_screen") }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Lists(innerPadding,viewModel.allLists,
            onItemClick = { list -> navController.navigate("items_screen/${list.id}/${list.name}") },
            onEditClick = { list -> openDialogForEdit.value = !openDialogForEdit.value
                listName.value = list.name
                listId.longValue= list.id},
            onDeleteClick = {list -> openDialogForDelete.value = !openDialogForDelete.value
                listName.value = list.name
                listId.longValue= list.id},
            initialScrollIndex = initialScrollIndex)
    }
    when {
        openDialogForEdit.value -> {
            DialogForEditingList(
                listName.value,
                onDismissRequest = { openDialogForEdit.value = false },
                onConfirmation = { newListName ->
                    if(newListName.isNotEmpty()) {
                        openDialogForEdit.value = false
                        viewModel.updateList(ListEntity(listId.longValue,newListName))
                    }
                    else {
                        println("error")
                    }
                },
            )
        }
        openDialogForDelete.value -> {
            DialogForDeletingList(
                listName.value,
                onDismissRequest = { openDialogForDelete.value = false },
                onConfirmation = {
                    openDialogForDelete.value = false
                    viewModel.deleteList(ListEntity(listId.longValue,listName.value))

                },
            )
        }
    }
}

@Composable
fun Lists(
    innerPadding: PaddingValues,
    items: LiveData<List<ListEntity>>,
    onItemClick: (ListEntity) -> Unit,
    onEditClick: (ListEntity) -> Unit,
    onDeleteClick: (ListEntity) -> Unit,
    initialScrollIndex: Int = 0
) {
    val itemList by items.observeAsState(initial = emptyList())

    val load = remember { mutableStateOf(false)}
    val progress = remember { mutableStateOf(true)}

    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))



    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            if (progress.value) {
                delay(1000)
                progress.value = false
            }
        }
    }

    LaunchedEffect(itemList) {
        coroutineScope.launch {
            if (itemList.isNotEmpty() && !load.value) {
                listState.scrollToItem(index = initialScrollIndex)
                load.value = true
            }
        }
    }

    if (itemList.isEmpty() || progress.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if(progress.value) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            else{
                Text("No items yet. Add one!")
            }
        }
    } else {

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            items(itemList, key = { it.id }) { item ->
                val totalCount by viewModel.getTotalCountLive(item.id).observeAsState(initial = 0)
                val completedCount by viewModel.getCompletedCountLive(item.id).observeAsState(initial = 0)
                var currentProgress by remember { mutableFloatStateOf(0f) }

                LaunchedEffect(completedCount, totalCount) {
                    currentProgress = if (totalCount > 0) {
                        completedCount.toFloat() / totalCount.toFloat()
                    } else {
                        0f
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onItemClick(item) } // Make the whole card clickable
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).width(300.dp)

                        ) { Card(
                            modifier = Modifier.fillMaxWidth()

                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (totalCount > 0){
                                LinearProgressIndicator(
                                    progress = { currentProgress },
                                    modifier = Modifier.width(200.dp).animateItem().padding(10.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.background,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedCount / $totalCount item/s are completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            else{
                                Text(
                                    text = "No items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                        }
                        }

                        Row {
                            IconButton(onClick = { onEditClick(item) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit"
                                )
                            }

                            IconButton(onClick = { onDeleteClick(item) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }

        }


    }
}

@Composable
fun DialogForEditingList(
    name: String = "",
    onDismissRequest: () -> Unit,
    onConfirmation:  (String) -> Unit,
) {
    var listName by remember { mutableStateOf(name) }
    val isConfirmButtonEnabled = listName.isNotBlank()
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Edit List",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp
                )
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List Name") }, // Optional label for the text field
                    singleLine = true, // Ensures the text field is a single line
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirmation(listName) },
                        modifier = Modifier.padding(8.dp),
                        enabled = isConfirmButtonEnabled,
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun DialogForDeletingList(
    name: String = "",
    onDismissRequest: () -> Unit,
    onConfirmation:  () -> Unit,
) {
    val listName by remember { mutableStateOf(name) }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Delete List ",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 22.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Are you sure you want to delete \"$listName\" ? ",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),

                        ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


@Composable
fun AddListButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = { onClick() },
        icon = { Icon(Icons.Filled.Add, "Extended floating action button.") },
        text = { Text(text = "Add List") },
    )
}

@Composable
fun DialogForAddingList(
    onDismissRequest: () -> Unit,
    onConfirmation:  (String) -> Unit,
) {
    var listName by remember { mutableStateOf("") }
    val isConfirmButtonEnabled = listName.isNotBlank()
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Add List",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp
                )
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List Name") }, // Optional label for the text field
                    singleLine = true, // Ensures the text field is a single line
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirmation(listName) },
                        modifier = Modifier.padding(8.dp),
                        enabled = isConfirmButtonEnabled,
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


//##-- list screen --##
@Composable
fun ListsScreen(navController: NavController, initialScrollIndex: Int = 0) {
    val openDialogForAdd = remember { mutableStateOf(false) }


    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))

    Scaffold(
        topBar = {
            ListAppBar(navController = navController, initialScrollIndex = initialScrollIndex)
        },
        floatingActionButton = {
            AddListButton(onClick = { openDialogForAdd.value = !openDialogForAdd.value })
        }

    ) { innerPadding ->
        // Content of the screen goes here.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp), // Add our own additional padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Your lists will appear here.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
    when {
        openDialogForAdd.value -> {
            DialogForAddingList(
                onDismissRequest = { openDialogForAdd.value = false },
                onConfirmation = { newListName ->
                    if(newListName.isNotEmpty()) {
                        openDialogForAdd.value = false
                        viewModel.addList(newListName)
                    }
                    else {
                        println("error")
                    }
                },
            )
        }
    }
}



