package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "agents")
data class Agent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val providerType: String = "GEMINI",
    val description: String = "",
    val icon: String = "person"
)

@Serializable
@Entity(tableName = "instructions")
data class Instruction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: Long,
    val name: String,
    val systemPrompt: String = "",
    val modelName: String = "gemini-3.5-flash",
    val responseType: String = "TEXT" // TEXT, IMAGE
)

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: Long,
    val instructionId: Long,
    val role: String, // USER, ASSISTANT
    val content: String,
    val type: String = "TEXT", // TEXT, IMAGE
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
