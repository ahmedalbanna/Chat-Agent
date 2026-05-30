package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.Instruction
import com.example.model.Message
import com.example.viewmodel.AgentViewModel
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    val annotatedString = remember(markdown) {
        buildAnnotatedString {
            // Very basic Markdown parser for bold (**) and italic (*)
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            val italicRegex = Regex("\\*(.*?)\\*")
            
            var lastIndex = 0
            val matches = (boldRegex.findAll(markdown) + italicRegex.findAll(markdown))
                .sortedBy { it.range.first }
                .toList()

            for (match in matches) {
                if (match.range.first < lastIndex) continue
                
                append(markdown.substring(lastIndex, match.range.first))
                
                val isBold = match.value.startsWith("**")
                withStyle(style = SpanStyle(
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (!isBold) FontStyle.Italic else FontStyle.Normal
                )) {
                    append(match.groupValues[1])
                }
                lastIndex = match.range.last + 1
            }
            append(markdown.substring(lastIndex))
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    agentViewModel: AgentViewModel,
    agentId: Long,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val instructions by agentViewModel.getInstructions(agentId).collectAsStateWithLifecycle()
    val agents by agentViewModel.allAgents.collectAsStateWithLifecycle()
    val agent = agents.find { it.id == agentId }
    val isTyping by chatViewModel.isTyping.collectAsStateWithLifecycle()
    val selectedId by chatViewModel.selectedInstructionId.collectAsStateWithLifecycle()

    val agentColor = MaterialTheme.colorScheme.primary

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        uri?.let {
            selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Column {
                            val activeInstruction = instructions.find { it.id == selectedId }
                            Text(agent?.name ?: activeInstruction?.name ?: "المحادثة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(if (isTyping) agentColor else Color(0xFF4CAF50), CircleShape))
                                Text(
                                    if (isTyping) "جاري التوليد..." else "متصل • ${activeInstruction?.modelName ?: "Gemini"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة")
                        }
                    },
                    actions = {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "المهام", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                instructions.forEach { instruction ->
                                    DropdownMenuItem(
                                        text = { Text(instruction.name) },
                                        onClick = {
                                            chatViewModel.selectInstruction(instruction.id)
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            if (selectedId == instruction.id) Icon(Icons.Default.Check, contentDescription = null)
                                            else Icon(if (instruction.responseType == "IMAGE") Icons.Default.Image else Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.size(80.dp).padding(bottom = 8.dp)) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null; selectedBitmap = null },
                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp).offset(x = 4.dp, y = (-4).dp).background(MaterialTheme.colorScheme.error, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "إرفاق", tint = MaterialTheme.colorScheme.primary)
                        }
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("اكتب رسالتك هنا...", fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || selectedBitmap != null) {
                                    chatViewModel.sendMessage(inputText, selectedBitmap)
                                    inputText = ""
                                    selectedImageUri = null
                                    selectedBitmap = null
                                }
                            },
                            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message, agentName = agent?.name ?: "الوكيل", agentColor = agentColor)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, agentName: String, agentColor: Color) {
    val isUser = message.role == "USER"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = agentColor)
                Text(
                    text = agentName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
        }
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
            contentColor = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.imagePath != null) {
                    AsyncImage(
                        model = message.imagePath,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .fillMaxWidth(0.8f)
                            .heightIn(max = 300.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                if (message.content.isNotBlank()) {
                    MarkdownText(
                        markdown = message.content,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                    }
                    
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.content)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الرسالة"))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
                    }

                    if (message.imagePath != null) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    saveImageLocally(context, message.imagePath)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "تنزيل", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

suspend fun saveImageLocally(context: android.content.Context, imageUrl: String) {
    withContext(Dispatchers.IO) {
        try {
            val fileName = "AI_Agent_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI_Agent")
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                val outputStream = resolver.openOutputStream(uri)
                val inputStream: InputStream = if (imageUrl.startsWith("http")) {
                    URL(imageUrl).openStream()
                } else {
                    File(imageUrl).inputStream()
                }
                
                inputStream.use { input ->
                    outputStream?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم حفظ الصورة في الاستوديو", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "فشل تنزيل الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
