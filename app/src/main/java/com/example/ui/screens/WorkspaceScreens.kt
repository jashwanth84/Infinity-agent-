package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.model.AppMode
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val selectedId by viewModel.selectedProjectId.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Coding Projects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your active workspace repositories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyanBright, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Project", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(projects) { project ->
                val isSelected = project.id == selectedId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.selectProject(project.id) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentCyanBright else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    color = if (isSelected) AccentCyanBright.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCyanBright.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) AccentCyanBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = project.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentIndigo.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = project.language,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentIndigo,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Project",
                                tint = AccentCyanBright,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onCreate = { name, desc, lang ->
                viewModel.createNewProject(name, desc, lang)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.currentProjectFiles.collectAsState()
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<ProjectFileEntity?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalFile(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Project File Explorer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${files.size} source & resource files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }

                Button(
                    onClick = { showNewFileDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New File")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.setActiveFile(file)
                            viewModel.setMode(AppMode.CODER)
                        }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconColor = when (file.language.lowercase()) {
                            "java" -> AccentAmber
                            "c++", "cpp" -> AccentCyanBright
                            "xml" -> AccentPurple
                            "kotlin", "kt" -> AccentIndigo
                            "python", "py" -> AccentGreen
                            else -> AccentRose
                        }

                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = file.path,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { fileToRename = file },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { fileToDelete = file },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = AccentRose,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        CreateFileDialog(
            onCreate = { name, path, lang ->
                viewModel.createNewFile(name, path, "// $name source file\n", lang)
                showNewFileDialog = false
            },
            onDismiss = { showNewFileDialog = false }
        )
    }

    if (fileToRename != null) {
        RenameFileDialog(
            file = fileToRename!!,
            onRename = { newName, newPath ->
                viewModel.renameFile(fileToRename!!.id, newName, newPath)
                fileToRename = null
            },
            onDismiss = { fileToRename = null }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete ${fileToDelete!!.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!.id)
                        fileToDelete = null
                    }
                ) {
                    Text("Delete", color = AccentRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val runs by viewModel.agentRepository.allRuns.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Execution History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Autonomous agent runs & multi-model audit logs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (runs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No recorded runs yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(runs) { run ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentPurple.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = run.mode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentPurple,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(run.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = run.taskPrompt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    onCreate: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("Java + C++") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("New Coding Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text("Primary Language (e.g. Kotlin, C++, Java)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotBlank()) onCreate(name, desc, lang) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyanBright, contentColor = Color.Black)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFileDialog(
    onCreate: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("Kotlin") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Create New File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (path.isEmpty()) path = "src/$it"
                    },
                    label = { Text("File Name (e.g. Utils.cpp)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Path in Project") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text("Language") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotBlank()) onCreate(name, path, lang) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameFileDialog(
    file: ProjectFileEntity,
    onRename: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var newPath by remember { mutableStateOf(file.path) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Rename File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPath,
                    onValueChange = { newPath = it },
                    label = { Text("New Path") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { if (newName.isNotBlank()) onRename(newName, newPath) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyanBright, contentColor = Color.Black)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
