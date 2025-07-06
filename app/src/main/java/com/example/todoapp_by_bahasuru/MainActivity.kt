package com.example.todoapp_by_bahasuru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.todoapp_by_bahasuru.ui.theme.ToDoApp_by_BahasuruTheme
import com.google.firebase.auth.FirebaseAuth

/**
 * Created by Bahasuru Nayanakantha
 */


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val googleAuthClient = GoogleAuthClient(this)
        setContent {
            val dark = isSystemInDarkTheme()
            val darkMode = remember { mutableStateOf(dark) }
            val toggleTheme: () -> Unit = {
                darkMode.value = !darkMode.value
            }
            ToDoApp_by_BahasuruTheme(darkTheme = darkMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    AppNavigation(googleAuthClient = googleAuthClient , onToggle = toggleTheme, isDark = darkMode.value)

                }
            }
        }
    }

}

//##---Components---###

//--Home page--

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(navController: NavController, onToggle: () -> Unit, isDark : Boolean) {

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val firebaseAuth = FirebaseAuth.getInstance()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                title = {
                    Text(
                        "ToDo App",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onToggle) {
                        if(isDark){
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_light_mode_24),
                                contentDescription = "Localized description"
                            )
                        }
                        else{
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_dark_mode_24),
                                contentDescription = "Localized description"
                            )

                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("account_screen") }) {
                        if(firebaseAuth.currentUser != null){
                            val painter = rememberAsyncImagePainter(model = firebaseAuth.currentUser?.photoUrl)
                            Image(
                                painter = painter,
                                contentDescription = "User Profile Picture",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                        } else{
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Localized description"
                            )
                        }

                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoImage()

                Spacer(modifier = Modifier.height(16.dp))
                if(firebaseAuth.currentUser != null){
                    val fullName = firebaseAuth.currentUser?.displayName
                    val firstName = fullName?.split(" ")?.firstOrNull() ?: "User"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        GreetingWithUser(name = firstName)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Greeting()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    About()
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoToListsButton(navController = navController)
            }
        }

    }

}

@Composable
fun LogoImage() {
    Image(
        painter = painterResource(id = R.drawable.list), // your PNG name without extension
        contentDescription = "App Logo",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(200.dp)
            .width(200.dp)
            .padding(16.dp)
    )
}
@Composable
fun GreetingWithUser(name: String) {
    Text(
        text = "Hello $name !",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center
    )
}


@Composable
fun Greeting() {
    Text(
        text = "Welcome to ToDo App",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun About() {
    Text(
        text = "Created by Bahasuru Nayanakantha ",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun GoToListsButton(navController: NavController) {
    Button(
        onClick = { navController.navigate("lists_screen") },
        modifier = Modifier
            .padding(50.dp)
            .fillMaxWidth(0.7f)
            .height(70.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        Text(
            text = stringResource(R.string.go_to_lists),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 22.sp
        )
    }
}



//##--- The Screens ---##
@Composable
fun HomeScreen(navController: NavController, onToggle: () -> Unit,isDark : Boolean) {

    HomeAppBar(navController= navController, onToggle =onToggle, isDark=isDark)
}


// ##--- Navigation Setup ---##
@Composable
fun AppNavigation(googleAuthClient: GoogleAuthClient, onToggle: () -> Unit, isDark : Boolean ) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home_screen") {
        //home page
        composable("home_screen") {
            HomeScreen(navController = navController, onToggle =onToggle, isDark=isDark)
        }
        //list page
        composable("lists_screen") {
            ListsScreen(navController = navController)
        }

        //used for search
        composable(
            "lists_screen?itemIndex={itemIndex}",
            arguments = listOf(
                navArgument("itemIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val itemIndex = backStackEntry.arguments?.getInt("itemIndex")

            if (itemIndex != null ) {
                ListsScreen(navController = navController, initialScrollIndex = itemIndex)
            }
        }

        composable(
            "items_screen/{listId}/{listName}?itemIndex={itemIndex}",
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("listName") { type = NavType.StringType },
                navArgument("itemIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId")
            val listName = backStackEntry.arguments?.getString("listName")
            val itemIndex = backStackEntry.arguments?.getInt("itemIndex")

            if (listId != null && listName != null && itemIndex != null) {
                ItemScreen(navController = navController, listId = listId, listName = listName, initialScrollIndex = itemIndex)
            }
        }

        composable("search_screen") {
            SearchScreen(navController = navController)
        }


        // task page
        composable(
            "items_screen/{listId}/{listName}",
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType },
                navArgument("listName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId")
            val listName = backStackEntry.arguments?.getString("listName")

            if (listId != null && listName != null) {
                ItemScreen(navController = navController, listId = listId, listName = listName)
            }
        }

        composable("account_screen") {
            AccountScreen(navController = navController,googleAuthClient = googleAuthClient)
        }
    }
}




//##---Preview---##

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    ToDoApp_by_BahasuruTheme {
        Column {
            ListsScreen(navController = rememberNavController())
        }
    }
}