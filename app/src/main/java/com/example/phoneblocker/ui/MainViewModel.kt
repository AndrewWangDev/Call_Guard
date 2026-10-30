package com.example.phoneblocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phoneblocker.data.local.dao.BlockLogDao
import com.example.phoneblocker.data.local.dao.RegexRuleDao
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import com.example.phoneblocker.service.CallScreeningServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val blockLogDao: BlockLogDao,
    private val regexRuleDao: RegexRuleDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val blockLogs = blockLogDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val regexRules = regexRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBlockingEnabled = userPreferencesRepository.isBlockingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBlockUnknownEnabled = userPreferencesRepository.isBlockUnknownEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isRoleHeld = MutableStateFlow(true)
    val isRoleHeld: StateFlow<Boolean> = _isRoleHeld.asStateFlow()

    fun updateRoleState(isHeld: Boolean) {
        _isRoleHeld.value = isHeld
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveBlockingEnabled(enabled)
        }
    }

    fun setBlockUnknownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveBlockUnknownEnabled(enabled)
        }
    }

    fun addRule(pattern: String, type: RuleType) {
        viewModelScope.launch {
            val cleanPattern = pattern.trim()
            if (cleanPattern.isNotBlank()) {
                regexRuleDao.insertRule(RegexRule(pattern = cleanPattern, type = type))
                CallScreeningServiceImpl.clearPatternCache()
            }
        }
    }

    fun deleteRule(rule: RegexRule) {
        viewModelScope.launch {
            regexRuleDao.deleteRule(rule)
            CallScreeningServiceImpl.clearPatternCache()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            blockLogDao.clearLogs()
        }
    }
}

class MainViewModelFactory(
    private val blockLogDao: BlockLogDao,
    private val regexRuleDao: RegexRuleDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(blockLogDao, regexRuleDao, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
