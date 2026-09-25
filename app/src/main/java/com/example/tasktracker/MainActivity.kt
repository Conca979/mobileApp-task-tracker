package com.example.tasktracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Task(val id: String, val title: String, var isDone: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskTrackerApp(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerApp(context: Context) {
    val sharedPreferences = context.getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)

    // Load tasks
    val initialTasks = remember {
        val tasksString = sharedPreferences.getString("tasks", "[]") ?: "[]"
        val jsonArray = JSONArray(tasksString)
        val list = mutableListOf<Task>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            list.add(
                Task(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    isDone = jsonObject.getBoolean("isDone")
                )
            )
        }
        list
    }

    var tasks by remember { mutableStateOf(initialTasks) }
    var showDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }

    // Save tasks
    fun saveTasks(updatedTasks: List<Task>) {
        val jsonArray = JSONArray()
        for (task in updatedTasks) {
            val jsonObject = JSONObject()
            jsonObject.put("id", task.id)
            jsonObject.put("title", task.title)
            jsonObject.put("isDone", task.isDone)
            jsonArray.put(jsonObject)
        }
        sharedPreferences.edit().putString("tasks", jsonArray.toString()).apply()
        tasks = updatedTasks.toList() // Trigger recomposition
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks yet. Add one!")
                }
            } else {
                LazyColumn {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onCheckedChange = { isChecked ->
                                val updatedTasks = tasks.map {
                                    if (it.id == task.id) it.copy(isDone = isChecked) else it
                                }
                                saveTasks(updatedTasks)
                            },
                            onDelete = {
                                val updatedTasks = tasks.filter { it.id != task.id }
                                saveTasks(updatedTasks)
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    newTaskTitle = ""
                },
                title = { Text("Add New Task") },
                text = {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val newTask = Task(
                                    id = UUID.randomUUID().toString(),
                                    title = newTaskTitle.trim(),
                                    isDone = false
                                )
                                saveTasks(tasks + newTask)
                                showDialog = false
                                newTaskTitle = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            newTaskTitle = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TaskRow(task: Task, onCheckedChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
        }
    }
}
