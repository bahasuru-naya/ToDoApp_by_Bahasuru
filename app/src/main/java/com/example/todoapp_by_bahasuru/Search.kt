package com.example.todoapp_by_bahasuru

import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.todoapp_by_bahasuru.data.AppDatabase
import com.example.todoapp_by_bahasuru.data.ListEntity
import com.example.todoapp_by_bahasuru.data.TodoItemEntity
import com.example.todoapp_by_bahasuru.data.TodoListRepository
import com.example.todoapp_by_bahasuru.data.TodoListViewModel
import com.example.todoapp_by_bahasuru.data.TodoListViewModelFactory


//search result of list
@Composable
fun SearchListsResult(
    innerPadding: PaddingValues,
    items: LiveData<List<ListEntity>>,
    onItemClick: (Int) -> Unit
) {

    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))

    val listIndex by viewModel.reversedIndexList.observeAsState(emptyList())

    val itemList by items.observeAsState(initial = emptyList())

    if (itemList.isEmpty()) {
        Box(
            modifier = Modifier
                .padding(innerPadding).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("No lists found")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(itemList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = {
                        val index = listIndex.firstOrNull { it.second == item.id }?.first
                        if (index != null) {
                            onItemClick(index)
                        }
                    } // Make the whole card clickable
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }


        }


    }
}

//search result of items
@Composable
fun SearchItemsResult(
    innerPadding: PaddingValues,
    items: LiveData<List<TodoItemEntity>>,
    onItemClick: (TodoItemEntity) -> Unit,
) {

    val itemList by items.observeAsState(initial = emptyList())

    if (itemList.isEmpty()) {
        Box(
            modifier = Modifier
                .padding(innerPadding).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("No items found ")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(itemList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { onItemClick(item)  } // Make the whole card clickable
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(4.dp).height(120.dp),
                            verticalArrangement = Arrangement.Center,

                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(4.dp) )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }


        }


    }
}



//search screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(), db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory = TodoListViewModelFactory(repository))

    val searchQuery by viewModel.searchQuery.observeAsState("")
    val searchListResults by viewModel.searchListResults.observeAsState(emptyList())
    val searchItemResults by viewModel.getItemResults.observeAsState(emptyList())

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = {
                    Text(
                        "Search",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary, // or any color
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Lists",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        textAlign = TextAlign.Center
                    )

                    SearchListsResult(
                        innerPadding = PaddingValues(4.dp),
                        items = MutableLiveData(searchListResults),
                        onItemClick = { index ->
                            keyboardController?.hide()
                            navController.navigate("lists_screen?itemIndex=$index")
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary, // or any color
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        textAlign = TextAlign.Center

                    )

                    SearchItemsResult(
                        innerPadding = PaddingValues(4.dp),
                        items = MutableLiveData(searchItemResults),
                        onItemClick = { item ->
                            keyboardController?.hide()
                            viewModel.getListNameById(item.listId){name ->
                            navController.navigate("items_screen/${item.listId}/${name}?itemIndex=${item.orderIndex}")
                            }
                        }
                    )
                }
            }
        }
    }
}
