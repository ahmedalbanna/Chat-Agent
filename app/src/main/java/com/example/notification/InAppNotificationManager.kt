package com.example.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class NotificationType {
    NEW_MESSAGE,
    LONG_RESPONSE
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType,
    val agentName: String = "",
    val read: Boolean = false
)

object InAppNotificationManager {
    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _incomingNotification = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 64)
    val incomingNotification = _incomingNotification.asSharedFlow()

    fun postNotification(title: String, message: String, type: NotificationType, agentName: String = "") {
        val notification = InAppNotification(
            title = title,
            message = message,
            type = type,
            agentName = agentName
        )
        // Add to history (newest first)
        _notifications.value = listOf(notification) + _notifications.value
        // Emit for the live in-app animated banner overlay
        _incomingNotification.tryEmit(notification)
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
    }

    fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }
}
