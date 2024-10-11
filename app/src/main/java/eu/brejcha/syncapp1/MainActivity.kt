package eu.brejcha.syncapp1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.brejcha.syncapp1.ui.theme.SyncApp1Theme
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.provider.DocumentsContract
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import eu.brejcha.syncapp1.ui.theme.bannerColor
import eu.brejcha.syncapp1.ui.theme.pathBg1
import eu.brejcha.syncapp1.ui.theme.pathBg2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncApp1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        AppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val paths = remember { mutableStateListOf<Path>() }

    val context = LocalContext.current
    var showRemovePathDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        paths.addAll(loadPathsFromDb(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (DocumentsContract.isTreeUri(uri)) {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val pathStr = docId.split(":")[1]
                    val path = Path(uri.toString(), pathStr, -1L)
                    paths.add(path)
                    savePathToDb(context, path)
                } else {
                    val pathStr = DocumentsContract.getDocumentId(uri)
                    val path = Path(uri.toString(), pathStr, -1L)
                    paths.add(path)
                    savePathToDb(context, path)
                }
            }
        }
    }

    var refreshList by remember { mutableStateOf(false) }

    if (showRemovePathDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePathDialog = false },
            title = { Text("Select Path") },
            text = {
                Column {
                    paths.forEach { path ->
                        Text(
                            text = path.path,
                            modifier = Modifier.clickable {
                                paths.remove(path)
                                removePathFromDb(context, path)
                                showRemovePathDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRemovePathDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var reportError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (reportError) {
        AlertDialog(
            onDismissRequest = { reportError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { reportError = false }) {
                    Text("OK")
                }
            }
        )
    }

    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) {
        SettingsDialog(settings = loadSettingsFromDb(context), onSettingsChange = { newSettings ->
            saveSettingsToDb(context, newSettings)
            showSettings = false
        }, onDismiss = { showSettings = false })
    }

    Column(modifier = Modifier) {
        Row( verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .background(if (refreshList) bannerColor else bannerColor) // just for trigger recomposition
            .fillMaxWidth()
            .requiredHeight(40.dp)
            //style = TextStyle(color = if (refreshList) bannerColor else bannerColor)
        ) {
            Text(text = "SyncApp", style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    fontSize = 20.sp,
                                                    color = Color.Black))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    showSettings = true
                }, modifier = Modifier
                    .size(30.dp)
                    //.background(Color.White, CircleShape)
                    //.padding(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Configure",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        paths.forEachIndexed { index, path ->
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(if (index % 2 == 0) pathBg1 else pathBg2)
            ){
                Column(modifier = Modifier) {
                    Text( text = "Path: ${path.path}")
                    if (path.progress < 100) {
                        Text(text = "Progress: ${path.progress}%")
                    } else {
                        Text(
                            text = "Last update: ${
                                if (path.timestamp == -1L) "never" else timestampToString(
                                    path.timestamp
                                )
                            }"
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                ProcessBackupButton(onClick = {
                    try {
                        syncPathWithSmb(path, context, onProcessUpdate = { progress ->
                            path.progress = progress
                            refreshList = !refreshList
                        })
                        path.timestamp = System.currentTimeMillis()
                        updatePathInDb(context, path)
                        refreshList = !refreshList
                    } catch (e: SyncAppException) {
                        // Handle the exception
                        reportError = true
                        errorMessage = e.message ?: "Unknown error"
                    }
                })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        // putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                    }
                    launcher.launch(intent)
                }) {
                Text("Add Path")
            }
            Button(onClick = { showRemovePathDialog = true }) {
                Text("Remove Path")
            }
        }
    }
}


@Composable
fun ProcessBackupButton(onClick: () -> Unit) {
    var processing by remember { mutableStateOf(false) }
    val angle = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = processing) {
        if (processing) {
            angle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                )
            )
        } else {
            angle.snapTo(0f)
        }
    }

    IconButton(
        onClick = {
            processing = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    onClick()
                    processing = false
                }
            }
        }, modifier = Modifier
            .size(35.dp)
            .background(if (processing) Color.White else Color.White, CircleShape)
            //.padding(1.dp)
            .border(1.dp, Color.Black, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Backup",
            tint = Color.Blue,
            modifier = Modifier
                .size(35.dp)
                .rotate(angle.value)
        )
    }
}


@Composable
fun SettingsDialog(
    settings: eu.brejcha.syncapp1.Settings,
    onSettingsChange: (eu.brejcha.syncapp1.Settings) -> Unit,
    onDismiss: () -> Unit
) {
    var updatedSettings by remember { mutableStateOf(settings) }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Settings",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Setting fields (e.g., TextField, Switch)
                TextField(
                    value = updatedSettings.serverIp,
                    onValueChange = { updatedSettings = updatedSettings.copy(serverIp = it) },
                    label = { Text("Server IP") }
                )
                TextField(
                    value = updatedSettings.serverPort.toString(),
                    onValueChange = { updatedSettings = updatedSettings.copy(serverPort = it.toIntOrNull() ?: 0) },
                    label = { Text("Server Port") }
                )
                TextField(
                    value = updatedSettings.username,
                    onValueChange = { updatedSettings = updatedSettings.copy(username = it) },
                    label = { Text("Username") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = updatedSettings.password,
                        onValueChange = { updatedSettings = updatedSettings.copy(password = it) },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                             contentDescription = if (passwordVisible) "Hide Password" else "Show Password")
                    }
                }
                TextField(
                    value = updatedSettings.domain,
                    onValueChange = { updatedSettings = updatedSettings.copy(domain = it) },
                    label = { Text("Domain") }
                )
                TextField(
                    value = updatedSettings.shareName,
                    onValueChange = { updatedSettings = updatedSettings.copy(shareName = it) },
                    label = { Text("Share Name") }
                )
                TextField(
                    value = updatedSettings.sharePath,
                    onValueChange = { updatedSettings = updatedSettings.copy(sharePath = it) },
                    label = { Text("Share Path") }
                )
                // ... other settings fields
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSettingsChange(updatedSettings) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}