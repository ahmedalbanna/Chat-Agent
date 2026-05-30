package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.remote.*
import com.example.data.repository.AgentRepository
import com.example.model.Instruction
import com.example.model.Message
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: AgentRepository,
    private val agentId: Long,
    private val initialInstructionId: Long
) : ViewModel() {

    private val _selectedInstructionId = MutableStateFlow(initialInstructionId)
    val selectedInstructionId = _selectedInstructionId.asStateFlow()

    init {
        if (initialInstructionId == 0L) {
            viewModelScope.launch {
                repository.getInstructions(agentId).firstOrNull()?.firstOrNull()?.let {
                    _selectedInstructionId.value = it.id
                }
            }
        }
    }

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    val messages: StateFlow<List<Message>> = _selectedInstructionId
        .flatMapLatest { instructionId ->
            repository.getMessages(agentId, instructionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectInstruction(id: Long) {
        _selectedInstructionId.value = id
    }

    fun sendMessage(content: String, bitmap: Bitmap? = null) {
        if (content.isBlank() && bitmap == null) return

        val instructionId = _selectedInstructionId.value
        viewModelScope.launch {
            val userMsg = Message(
                agentId = agentId,
                instructionId = instructionId,
                role = "USER",
                content = content,
                type = if (bitmap != null) "IMAGE" else "TEXT"
            )
            repository.insertMessage(userMsg)

            _isTyping.value = true
            try {
                val instruction = repository.getInstructionById(instructionId)
                val conversation = messages.value.takeLast(10) // Context window

                val parts = mutableListOf<Part>()
                parts.add(Part(text = content))
                if (bitmap != null) {
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64())))
                }

                val history = conversation.map { msg ->
                    Content(role = if (msg.role == "USER") "user" else "model", parts = listOf(Part(text = msg.content)))
                }

                val systemInstruction = instruction?.systemPrompt?.let {
                    if (it.isNotBlank()) Content(parts = listOf(Part(text = it))) else null
                }

                val modelName = instruction?.modelName ?: "gemini-3.5-flash"
                val responseType = instruction?.responseType ?: "TEXT"

                val generationConfig = if (responseType == "IMAGE") {
                    GenerationConfig(
                        responseModalities = listOf("TEXT", "IMAGE"),
                        imageConfig = ImageConfig()
                    )
                } else null

                val request = GenerateContentRequest(
                    contents = history + Content(role = "user", parts = parts),
                    systemInstruction = systemInstruction,
                    generationConfig = generationConfig
                )

                val response = RetrofitClient.service.generateContent(
                    model = modelName,
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = request
                )

                val aiResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                
                val aiMsg = Message(
                    agentId = agentId,
                    instructionId = instructionId,
                    role = "ASSISTANT",
                    content = aiResponseText
                )
                repository.insertMessage(aiMsg)

            } catch (e: Exception) {
                repository.insertMessage(Message(
                    agentId = agentId,
                    instructionId = instructionId,
                    role = "ASSISTANT",
                    content = "Error: ${e.message}"
                ))
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
