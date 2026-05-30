package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AgentRepository

class ViewModelFactory(private val repository: AgentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AgentViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AgentViewModel(repository) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                HistoryViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class ChatViewModelFactory(
    private val repository: AgentRepository,
    private val agentId: Long,
    private val instructionId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, agentId, instructionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
