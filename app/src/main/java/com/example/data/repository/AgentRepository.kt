package com.example.data.repository

import com.example.data.local.AgentDao
import com.example.data.local.InstructionDao
import com.example.data.local.MessageDao
import com.example.model.Agent
import com.example.model.Instruction
import com.example.model.Message
import kotlinx.coroutines.flow.Flow

class AgentRepository(
    private val agentDao: AgentDao,
    private val instructionDao: InstructionDao,
    private val messageDao: MessageDao
) {
    val allAgents: Flow<List<Agent>> = agentDao.getAllAgents()

    suspend fun getAgentById(id: Long) = agentDao.getAgentById(id)
    suspend fun insertAgent(agent: Agent) = agentDao.insertAgent(agent)
    suspend fun deleteAgent(agent: Agent) = agentDao.deleteAgent(agent)

    fun getInstructions(agentId: Long) = instructionDao.getInstructionsForAgent(agentId)
    suspend fun getInstructionById(id: Long) = instructionDao.getInstructionById(id)
    suspend fun insertInstruction(instruction: Instruction) = instructionDao.insertInstruction(instruction)
    suspend fun deleteInstruction(instruction: Instruction) = instructionDao.deleteInstruction(instruction)

    fun getMessages(agentId: Long, instructionId: Long) = messageDao.getMessagesForAgentAndInstruction(agentId, instructionId)
    suspend fun insertMessage(message: Message) = messageDao.insertMessage(message)
    suspend fun clearChat(agentId: Long) = messageDao.deleteMessagesForAgent(agentId)
}
