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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.todoapp_by_bahasuru.data.AppDatabase
import com.example.todoapp_by_bahasuru.data.TodoItemEntity
import com.example.todoapp_by_bahasuru.data.TodoListRepository
import com.example.todoapp_by_bahasuru.data.TodoListViewModel
import com.example.todoapp_by_bahasuru.data.TodoListViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


//--task page--
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemAppBar(navController: NavController, id: Long, lName: String , initialScrollIndex: Int = 0) {

    val openDialogForEdit = remember { mutableStateOf(false) }
    val openDialogForDelete = remember { mutableStateOf(false) }
    val itemTemp = remember { mutableStateOf(TodoItemEntity(0L,0L,"","",false,0)) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))
    viewModel.selectList(id)


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = {
                    Text(
                        " $lName ",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("lists_screen") }) {
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
        ItemLists(innerPadding,viewModel.currentListItems,
            onItemCheckedChange = { item, isChecked -> viewModel.updateTodoItem(item.copy(completed = isChecked)) },
            onUpClick = { item -> viewModel.fetchOrderIndex(item.id){orderIndex ->
                if (orderIndex != null) {
                    viewModel.swapWithPrevious(id,orderIndex)
                }
            }},
            onDownClick = {item -> viewModel.fetchOrderIndex(item.id){orderIndex ->
                if (orderIndex != null) {
                    viewModel.swapWithNext(id,orderIndex)
                }
            }},
            onEditClick = { item -> openDialogForEdit.value = !openDialogForEdit.value
                            itemTemp.value = item },
            onDeleteClick = {item-> openDialogForDelete.value = !openDialogForDelete.value
                            itemTemp.value = item },
            initialScrollIndex = initialScrollIndex)
    }
    when {
        openDialogForEdit.value -> {
            DialogForEditingItem(
                itemTemp.value.title,
                itemTemp.value.description,
                onDismissRequest = { openDialogForEdit.value = false },
                onConfirmation = { newItemName, newItemDescription ->
                    if(newItemName.isNotEmpty()) {
                        openDialogForEdit.value = false
                        viewModel.updateTodoItem(itemTemp.value.copy(title = newItemName, description = newItemDescription))

                    }
                    else {
                        println("error")
                    }
                },
            )
        }
        openDialogForDelete.value -> {
            DialogForDeletingItem(
                itemTemp.value.title,
                onDismissRequest = { openDialogForDelete.value = false },
                onConfirmation = {
                    openDialogForDelete.value = false
                    viewModel.deleteTodoItem(itemTemp.value)

                },
            )
        }
    }
}


@Composable
fun ItemLists(
    innerPadding: PaddingValues,
    items: LiveData<List<TodoItemEntity>>,
    onItemCheckedChange: (TodoItemEntity, Boolean) -> Unit,
    onUpClick: (TodoItemEntity) -> Unit,
    onDownClick: (TodoItemEntity) -> Unit,
    onEditClick: (TodoItemEntity) -> Unit, // Renamed for clarity
    onDeleteClick: (TodoItemEntity) -> Unit,
    initialScrollIndex: Int = 0
) {
    val itemList by items.observeAsState(initial = emptyList())

    // State to track the ID of the expanded item
    var expandedItemId by remember { mutableStateOf<Long?>(null) }

    val load = remember { mutableStateOf(false)}
    val progress = remember { mutableStateOf(true)}
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
                listState.animateScrollToItem(index = initialScrollIndex)
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
            itemsIndexed(itemList, key = { _, item -> item.id }) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.completed,
                            onCheckedChange = { isChecked ->
                                onItemCheckedChange(item, isChecked)
                            }
                        )
                        // Left side with Title and Description
                        Column(
                            modifier = Modifier.weight(1f)

                        ) { Card(
                            modifier = Modifier.fillMaxWidth()

                            ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(10.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if(item.description==""){
                                Text(
                                    text = "No Description",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontStyle = FontStyle.Italic
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )}
                            else{
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            }
                        }



                        // Right side: Conditionally show buttons
                        if (expandedItemId == item.id) {
                            // Expanded state: Show Edit and Delete buttons
                            Row(
                                modifier = Modifier.height(100.dp).width(90.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { onEditClick(item) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Item"
                                    )
                                }
                                IconButton(onClick = { onDeleteClick(item) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete Item"
                                    )
                                }
                            }
                        } else {
                            // Initial state: Show Checkbox and Up/Down buttons
                            Row(
                                modifier = Modifier.height(100.dp).width(90.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Spacer(modifier = Modifier.width(40.dp))

                                Column {
                                    if (index > 0) {
                                        IconButton(onClick = { onUpClick(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowUp,
                                                contentDescription = "Move Up"
                                            )
                                        }
                                    }

                                    if (index < itemList.size - 1) {
                                        IconButton(onClick = { onDownClick(item) }) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowDown,
                                                contentDescription = "Move Down"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { expandedItemId = if (expandedItemId == item.id) null else item.id }) {
                            if (expandedItemId == item.id) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Delete Item"
                                )

                            }
                            else {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Delete Item"
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
fun AddItemButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = { onClick() },
        icon = { Icon(Icons.Filled.Add, "Extended floating action button.") },
        text = { Text(text = "Add Item") },
    )
}

@Composable
fun DialogForAddingItem(
    onDismissRequest: () -> Unit,
    onConfirmation: (String, String) -> Unit, // Now takes name and description
) {
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }

    val isConfirmButtonEnabled = itemName.isNotBlank()

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp) // Increased height for new field
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
                    text = "Add Item",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)

                )

                OutlinedTextField(
                    value = itemDescription,
                    onValueChange = { if (it.length <= 60) itemDescription = it },
                    label = { Text("Description (optional)") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(100.dp)
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
                        onClick = { onConfirmation(itemName, itemDescription) },
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
fun DialogForEditingItem(
    name: String = "",
    description: String = "",
    onDismissRequest: () -> Unit,
    onConfirmation: (String, String) -> Unit, // Now takes name and description
) {
    var itemName by remember { mutableStateOf(name) }
    var itemDescription by remember { mutableStateOf(description) }

    val isConfirmButtonEnabled = itemName.isNotBlank()

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp) // Increased height for new field
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
                    text = "Edit Item",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                OutlinedTextField(
                    value = itemDescription,
                    onValueChange = { if (it.length <= 60) itemDescription = it },
                    label = { Text("Description (optional)") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(100.dp)
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
                        onClick = { onConfirmation(itemName, itemDescription) },
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
fun DialogForDeletingItem(
    name: String = "",
    onDismissRequest: () -> Unit,
    onConfirmation:  () -> Unit,
) {
    val itemName by remember { mutableStateOf(name) }
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
                    text = "Delete Item ",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 22.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Are you sure you want to delete \"$itemName\" ? ",
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


//###--task screen--##

@Composable
fun ItemScreen(navController: NavController,listId: Long,listName: String, initialScrollIndex: Int = 0) {
    val openDialogForAdd = remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))

    Scaffold(
        topBar = {
            ItemAppBar(navController = navController,listId,listName,initialScrollIndex)
        },
        floatingActionButton = {
            AddItemButton(onClick = { openDialogForAdd.value = !openDialogForAdd.value })
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
                text = "Your tasks will appear here.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
    when {
        openDialogForAdd.value -> {
            DialogForAddingItem(
                onDismissRequest = { openDialogForAdd.value = false },
                onConfirmation = { newItemName,newItemDescription ->
                    if(newItemName.isNotEmpty()) {
                        openDialogForAdd.value = false
                        viewModel.addTodoItem(listId,newItemName,newItemDescription)
                    }
                    else {
                        println("error")
                    }
                },
            )
        }
    }
}