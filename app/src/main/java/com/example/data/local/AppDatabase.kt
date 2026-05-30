package com.example.data.local

import androidx.room.*
import com.example.model.Agent
import com.example.model.Instruction
import com.example.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents")
    fun getAllAgents(): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: Long): Agent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent): Long

    @Delete
    suspend fun deleteAgent(agent: Agent)
}

@Dao
interface InstructionDao {
    @Query("SELECT * FROM instructions WHERE agentId = :agentId")
    fun getInstructionsForAgent(agentId: Long): Flow<List<Instruction>>

    @Query("SELECT * FROM instructions WHERE id = :id")
    suspend fun getInstructionById(id: Long): Instruction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: Instruction): Long

    @Delete
    suspend fun deleteInstruction(instruction: Instruction)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE agentId = :agentId AND instructionId = :instructionId ORDER BY timestamp ASC")
    fun getMessagesForAgentAndInstruction(agentId: Long, instructionId: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE agentId = :agentId")
    suspend fun deleteMessagesForAgent(agentId: Long)
}

@Database(entities = [Agent::class, Instruction::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun instructionDao(): InstructionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_agent_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
