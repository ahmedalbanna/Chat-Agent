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

    var agentName by remember(agent) { mutableStateOf(agent?.name ?: "") }
    var agentDesc by remember(agent) { mutableStateOf(agent?.description ?: "") }
    var agentColorHex by remember(agent) { mutableStateOf(agent?.colorHex ?: "#6750A4") }

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
                        Text("مظهر الوكيل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        TextField(
                            value = agentName,
                            onValueChange = { agentName = it },
                            label = { Text("الاسم") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        TextField(
                            value = agentDesc,
                            onValueChange = { agentDesc = it },
                            label = { Text("الوصف") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("لون الوكيل", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("#6750A4", "#4CAF50", "#FFC107", "#F44336", "#2196F3").forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color)), CircleShape)
                                        .border(if (agentColorHex == color) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        .clickable { agentColorHex = color }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                agent?.let {
                                    viewModel.updateAgent(it.copy(name = agentName, description = agentDesc, colorHex = agentColorHex))
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("تحديث الملف الشخصي")
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("إعدادات التعليمة النشطة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                                Text("نمط هجين", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("التعليمات (Capabilities)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("توليد ردود ذكية وتحليل عميق للبيانات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(instructions) { instruction ->
                InstructionCard(
                    instruction = instruction,
                    onDelete = { viewModel.deleteInstruction(instruction) }
                )
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("gemini-3.5-flash") }
        var type by remember { mutableStateOf("TEXT") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("تعليمة جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("النموذج", fontWeight = FontWeight.SemiBold)
                    val models = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-flash-image")
                    models.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { model = m }) {
                            RadioButton(selected = (model == m), onClick = { model = m })
                            Text(m)
                        }
                    }

                    Text("نوع الاستجابة", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (type == "TEXT"), onClick = { type = "TEXT" })
                        Text("نص")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = (type == "IMAGE"), onClick = { type = "IMAGE" })
                        Text("صورة")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addInstruction(agentId, name, model, type)
                        showAddDialog = false
                    }
                }) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun InstructionCard(instruction: Instruction, onDelete: () -> Unit) {
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
                Text("${instruction.modelName} • ${if (instruction.responseType == "TEXT") "نص" else "صورة"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}
