package com.streamhub.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdminManager {
    private const val DEFAULT_ADMIN_PIN = "1234"
    
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    fun verifyAndEnableAdmin(pin: String): Boolean {
        return if (pin == DEFAULT_ADMIN_PIN) {
            _isAdminMode.value = true
            true
        } else {
            false
        }
    }

    fun disableAdmin() {
        _isAdminMode.value = false
    }

    fun toggleAdminForTesting() {
        _isAdminMode.value = !_isAdminMode.value
    }
}
