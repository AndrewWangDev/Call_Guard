package com.example.phoneblocker.service

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.phoneblocker.data.local.AppDatabase
import com.example.phoneblocker.data.local.entity.BlockLog
import com.example.phoneblocker.data.local.entity.RuleType
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val repository = UserPreferencesRepository(context)
        val blockLogDao = database.blockLogDao()
        val regexRuleDao = database.regexRuleDao()

        // 1. Master Switch Check
        val isBlockingEnabled = runBlocking {
            repository.isBlockingEnabled.first()
        }

        if (!isBlockingEnabled) {
            respondToCall(callDetails, allowCallResponse())
            return
        }

        // 2. Phone Number Sanitization
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        // Remove "tel:" prefix if present (schemeSpecificPart usually handles this, but safer to be sure)
        // and remove all non-digit characters except '+'
        val sanitizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")

        if (sanitizedNumber.isBlank()) {
            respondToCall(callDetails, allowCallResponse())
            return
        }

        // 3. Matching Logic
        // We need to fetch rules blocking here to ensure synchronous decision
        val rules = runBlocking {
            regexRuleDao.getAllRules().first()
        }

        // Step A (Whitelist)
        val whitelistRules = rules.filter { it.type == RuleType.WHITELIST }
        for (rule in whitelistRules) {
            if (Regex(rule.pattern).matches(sanitizedNumber)) {
                respondToCall(callDetails, allowCallResponse())
                return
            }
        }

        // Step B (Blacklist)
        val blacklistRules = rules.filter { it.type == RuleType.BLACKLIST }
        for (rule in blacklistRules) {
            if (Regex(rule.pattern).matches(sanitizedNumber)) {
                // BLOCK
                respondToCall(callDetails, blockCallResponse())
                
                // Logging Action (Async)
                CoroutineScope(Dispatchers.IO).launch {
                    blockLogDao.insertLog(
                        BlockLog(
                            phoneNumber = sanitizedNumber,
                            timestamp = System.currentTimeMillis(),
                            reason = rule.pattern
                        )
                    )
                }
                return
            }
        }

        // Step C (Default)
        respondToCall(callDetails, allowCallResponse())
    }

    private fun allowCallResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private fun blockCallResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false) // User might want to see it in system log too, but prompt requested skip notification
            .setSkipNotification(true)
            .build()
    }
}
