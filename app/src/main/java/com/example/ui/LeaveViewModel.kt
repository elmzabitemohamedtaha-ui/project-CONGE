package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppAlert
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.data.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LeaveViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LeaveRepository.getInstance(application)
    
    // UI State Streams
    val allRequestsFlow: Flow<List<LeaveRequestEntity>> = repository.getAllRequestsFlow()
    val pendingRequestsFlow: Flow<List<LeaveRequestEntity>> = repository.getPendingRequestsFlow()
    val allApprovedRequestsFlow: Flow<List<LeaveRequestEntity>> = repository.getAllApprovedRequestsFlow()
    
    // Popup Alert State
    val activePopupAlert: StateFlow<AppAlert?> = repository.activePopupAlert
    
    // User-specific streams
    fun getEmployeeRequestsFlow(email: String): Flow<List<LeaveRequestEntity>> {
        return repository.getEmployeeRequestsFlow(email)
    }

    fun getAlertsForUserFlow(email: String): Flow<List<AppAlert>> {
        return repository.getAlertsForUserFlow(email)
    }

    fun getUnreadAlertsCountFlow(email: String): Flow<Int> {
        return repository.getUnreadAlertsCountFlow(email)
    }

    // Actions
    fun submitLeaveRequest(
        user: User,
        leaveType: String,
        startDateStr: String,
        endDateStr: String,
        reason: String,
        attachmentName: String? = null
    ) {
        viewModelScope.launch {
            repository.submitLeaveRequest(user = user, leaveType = leaveType, startDateStr = startDateStr, endDateStr = endDateStr, reason = reason, attachmentName = attachmentName)
        }
    }

    fun approveRequest(requestId: String, adminComment: String = "") {
        viewModelScope.launch {
            repository.approveRequest(requestId, adminComment)
        }
    }

    fun rejectRequest(requestId: String, adminComment: String = "") {
        viewModelScope.launch {
            repository.rejectRequest(requestId, adminComment)
        }
    }

    fun deleteLeaveRequest(request: LeaveRequestEntity) {
        viewModelScope.launch {
            repository.deleteLeaveRequest(request)
        }
    }

    // Alert Actions
    fun markAlertRead(alertId: String) {
        viewModelScope.launch {
            repository.markAlertRead(alertId)
        }
    }

    fun markAllAlertsRead(email: String) {
        viewModelScope.launch {
            repository.markAllAlertsRead(email)
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }

    fun clearAllAlerts(email: String) {
        viewModelScope.launch {
            repository.clearAllAlerts(email)
        }
    }
    
    fun checkForUnshownAlerts(email: String) {
        viewModelScope.launch {
            repository.checkForUnshownAlerts(email)
        }
    }

    fun dismissActiveAlert(alert: AppAlert) {
        viewModelScope.launch {
            repository.dismissActiveAlert(alert)
        }
    }

    fun clearActiveAlert() {
        repository.clearActiveAlert()
    }
}
