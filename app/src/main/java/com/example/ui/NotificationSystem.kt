package com.example.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationSystem {
    private val _notifications = MutableSharedFlow<NotificationMessage>(extraBufferCapacity = 1)
    val notifications = _notifications.asSharedFlow()

    fun sendNotification(title: String, message: String, isApproved: Boolean = true) {
        _notifications.tryEmit(NotificationMessage(title, message, isApproved))
    }
}

data class NotificationMessage(
    val title: String,
    val message: String,
    val isApproved: Boolean
)
