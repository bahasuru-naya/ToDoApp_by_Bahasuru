package com.example.todoapp_by_bahasuru

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.todoapp_by_bahasuru.data.AppDatabase
import com.example.todoapp_by_bahasuru.data.TodoListRepository
import com.example.todoapp_by_bahasuru.data.TodoListViewModel
import com.example.todoapp_by_bahasuru.data.TodoListViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AuthenticationScreen(googleAuthClient: GoogleAuthClient) {
    val context = LocalContext.current.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = TodoListRepository(db.listDao(),db.todoItemDao())
    val viewModel: TodoListViewModel = viewModel(factory =  TodoListViewModelFactory(repository))

    var isSignedIn by rememberSaveable { mutableStateOf(googleAuthClient.isSingedIn()) }

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var completedSignIn by remember { mutableStateOf(true) }

    var completedB1 by remember { mutableStateOf(true) }
    var completedB2 by remember { mutableStateOf(true) }
    var openDialog by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    // Observe the operation status to show feedback to the user
    viewModel.operationStatus.observe(lifecycleOwner) { result ->
        result.onSuccess { message ->
            println(message)
            completedB1 = true
            completedB2 = true
            openDialog = true
            isError = false
            msg = message

        }
        result.onFailure { error ->
            println(error)
            completedB1 = true
            completedB2 = true
            openDialog = true
            isError = false
            msg = error.toString()

        }
    }

    if (isSignedIn) {
        val firebaseAuth = FirebaseAuth.getInstance()
        completedSignIn = true
        SignedInView(
            username = firebaseAuth.currentUser?.displayName,
            email = firebaseAuth.currentUser?.email,
            image = firebaseAuth.currentUser?.photoUrl,
            completeB1 = completedB1,
            completeB2 = completedB2,
            onSignOut = {
                coroutineScope.launch {
                    googleAuthClient.signOut()
                    isSignedIn = false
                }
            },
            onLoad = {
                viewModel.restoreUserData(firebaseAuth.currentUser?.email)
                completedB1 = false

            },
            onSave = {
                viewModel.backupUserData(firebaseAuth.currentUser?.email)
                completedB2 = false

            }
        )
    } else {
        SignInView(completedSignIn = completedSignIn ,onSignIn = {
            coroutineScope.launch {
                completedSignIn = false
                isSignedIn = googleAuthClient.signIn()
               if(!isSignedIn){
                   openDialog = true
                   isError = true
                   msg = "Sign In Error. Please try again."
                   completedSignIn = true
               }
            }
        })
    }
    when {
        openDialog -> {
            DialogForMsg(
                msg = msg,
                isError = isError,
                onDismissRequest = { openDialog = false })
        }
    }
}

@Composable
fun SignedInView(
    username: String?,
    email: String?,
    image: Uri?,
    completeB1: Boolean,
    completeB2: Boolean,
    onSignOut: () -> Unit,
    onLoad: () -> Unit,
    onSave: () -> Unit
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        UserProfileSection(user = username, email = email, image = image)

        Button(
            onClick = onSignOut,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.7f)
                .height(50.dp)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Text(
                text = "Sign Out",
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Cloud Storage",
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick =  onLoad,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.5f)
                .height(60.dp)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            if(!completeB1){
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }else{
                Text(
                    text = "Restore Data",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = onSave,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.5f)
                .height(60.dp)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            if(!completeB2){
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }else {

                Text(
                    text = "Backup Data",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SignInView(completedSignIn: Boolean, onSignIn: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "To backup your lists and tasks in cloud, Connect your Google Account",
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.8f)
                .height(75.dp)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            if(completedSignIn) {
                Icon(
                    painter = painterResource(id = R.drawable.google_g),
                    modifier = Modifier.padding(4.dp),
                    contentDescription = "Google logo"
                )
                Text(
                    text = "Sign In With Google",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center

                )
            }else{
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun UserProfileSection(user: String?, email: String?, image: Uri?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val painter = rememberAsyncImagePainter(model = image)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painter,
                contentDescription = "User Profile Picture",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = user ?: "N/A", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = email ?: "N/A", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAppBar(navController: NavController, googleAuthClient: GoogleAuthClient) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Account", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home_screen") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                AuthenticationScreen(googleAuthClient = googleAuthClient)
            }
        }
    )
}


@Composable
fun DialogForMsg(
    msg: String = "",
    isError:Boolean,
    onDismissRequest: () -> Unit,
) {
    val message by remember { mutableStateOf(msg) }
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

                if(isError){
                Text(
                    text = "Error ",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 22.sp
                )}
                else{
                    Text(
                        text = "Operation Successful!",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 22.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = message,
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
                        Text("Okay")
                    }

                }
            }
        }
    }
}







//##--- The Screens ---##
@Composable
fun AccountScreen(navController: NavController,googleAuthClient: GoogleAuthClient) {
    AccountAppBar(navController= navController, googleAuthClient = googleAuthClient)
}