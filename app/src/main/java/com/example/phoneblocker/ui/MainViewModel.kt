package com.example.phoneblocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phoneblocker.data.local.dao.BlockLogDao
import com.example.phoneblocker.data.local.dao.RegexRuleDao
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
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

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveBlockingEnabled(enabled)
        }
    }

    fun addRule(pattern: String, type: RuleType) {
        viewModelScope.launch {
            regexRuleDao.insertRule(RegexRule(pattern = pattern, type = type))
        }
    }

    fun deleteRule(rule: RegexRule) {
        viewModelScope.launch {
            regexRuleDao.deleteRule(rule)
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
