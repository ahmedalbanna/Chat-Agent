package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Instruction
import com.example.viewmodel.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEditorScreen(
    viewModel: AgentViewModel,
    agentId: Long,
    onBack: () -> Unit
) {
    val instructions by viewModel.getInstructions(agentId).collectAsStateWithLifecycle()
    val agents by viewModel.allAgents.collectAsStateWithLifecycle()
    val agent = agents.find { it.id == agentId }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInstruction by remember { mutableStateOf<Instruction?>(null) }

    var agentName by remember(agent) { mutableStateOf(agent?.name ?: "") }
    var agentDesc by remember(agent) { mutableStateOf(agent?.description ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الوكيل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة تعليمة")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("بيانات الوكيل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        TextField(
                            value = agentName,
                            onValueChange = { agentName = it },
                            label = { Text("اسم الوكيل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        TextField(
                            value = agentDesc,
                            onValueChange = { agentDesc = it },
                            label = { Text("وصف الوكيل") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                agent?.let {
                                    viewModel.updateAgent(it.copy(name = agentName, description = agentDesc))
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("تحديث البيانات")
                        }
                    }
                }
            }
            item {
                Text(
                    "التعليمات والقدرات",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(instructions) { instruction ->
                InstructionCard(
                    instruction = instruction,
                    onEdit = { editingInstruction = instruction },
                    onDelete = { viewModel.deleteInstruction(instruction) }
                )
            }
        }
    }

    if (showAddDialog) {
        InstructionDialog(
            title = "تعليمة جديدة",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, prompt, model, type ->
                viewModel.addInstruction(agentId, name, prompt, model, type)
            }
        )
    }

    if (editingInstruction != null) {
        InstructionDialog(
            title = "تعديل التعليمة",
            initialInstruction = editingInstruction,
            onDismiss = { editingInstruction = null },
            onConfirm = { name, prompt, model, type ->
                editingInstruction?.let {
                    viewModel.updateInstruction(it.copy(name = name, systemPrompt = prompt, modelName = model, responseType = type))
                }
            }
        )
    }
}

@Composable
fun InstructionDialog(
    title: String,
    initialInstruction: Instruction? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialInstruction?.name ?: "") }
    var prompt by remember { mutableStateOf(initialInstruction?.systemPrompt ?: "") }
    var model by remember { mutableStateOf(initialInstruction?.modelName ?: "gemini-3.5-flash") }
    var type by remember { mutableStateOf(initialInstruction?.responseType ?: "TEXT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("اسم القدرة") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    TextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("التعليمة (System Prompt)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                item {
                    Text("النموذج المستخدم", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    val models = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-flash-image")
                    models.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { model = m }) {
                            RadioButton(selected = (model == m), onClick = { model = m })
                            Text(m, fontSize = 14.sp)
                        }
                    }
                }
                item {
                    Text("نوع مخرجات القدرة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (type == "TEXT"), onClick = { type = "TEXT" })
                        Text("نصي")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = (type == "IMAGE"), onClick = { type = "IMAGE" })
                        Text("صورة")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name, prompt, model, type)
                    onDismiss()
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun InstructionCard(instruction: Instruction, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (instruction.responseType == "IMAGE") Icons.Default.Image else Icons.Default.ChatBubble,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(instruction.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${instruction.modelName} • ${if (instruction.responseType == "TEXT") "نصي" else "صورة"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(20.dp))
            }
        }
    }
}
