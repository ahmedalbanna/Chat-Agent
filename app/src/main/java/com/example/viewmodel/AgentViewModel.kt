package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AgentRepository
import com.example.model.Agent
import com.example.model.Instruction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgentViewModel(private val repository: AgentRepository) : ViewModel() {

    val allAgents: StateFlow<List<Agent>> = repository.allAgents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getInstructions(agentId: Long): StateFlow<List<Instruction>> {
        return repository.getInstructions(agentId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addAgent(name: String, description: String = "") {
        viewModelScope.launch {
            val agentId = repository.insertAgent(Agent(name = name, description = description))
            // Add a default instruction
            repository.insertInstruction(Instruction(agentId = agentId, name = "التعليمة الافتراضية", systemPrompt = "أنت مساعد ذكي ولطيف."))
        }
    }

    fun updateAgent(agent: Agent) {
        viewModelScope.launch {
            repository.updateAgent(agent)
        }
    }

    fun deleteAgent(agent: Agent) {
        viewModelScope.launch {
            repository.deleteAgent(agent)
        }
    }

    fun addInstruction(agentId: Long, name: String, prompt: String, model: String, type: String) {
        viewModelScope.launch {
            repository.insertInstruction(
                Instruction(
                    agentId = agentId,
                    name = name,
                    systemPrompt = prompt,
                    modelName = model,
                    responseType = type
                )
            )
        }
    }

    fun updateInstruction(instruction: Instruction) {
        viewModelScope.launch {
            repository.updateInstruction(instruction)
        }
    }

    fun deleteInstruction(instruction: Instruction) {
        viewModelScope.launch {
            repository.deleteInstruction(instruction)
        }
    }
}
