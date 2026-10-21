package com.example.phoneblocker.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.phoneblocker.data.local.AppDatabase
import com.example.phoneblocker.data.local.entity.BlockLog
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class CallScreeningServiceImpl : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // Only screen incoming calls; never block outgoing calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allowCallResponse())
            return
        }

        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val repository = UserPreferencesRepository(context)
        val blockLogDao = database.blockLogDao()
        val regexRuleDao = database.regexRuleDao()

        serviceScope.launch {
            try {
                // 1. Master Switch Check
                val isBlockingEnabled = repository.isBlockingEnabled.firstOrNull() ?: false
                if (!isBlockingEnabled) {
                    respondToCall(callDetails, allowCallResponse())
                    return@launch
                }

                // 2. Phone Number Sanitization
                val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
                val sanitizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                val digitsOnly = sanitizedNumber.removePrefix("+")

                if (sanitizedNumber.isBlank()) {
                    respondToCall(callDetails, allowCallResponse())
                    return@launch
                }

                // 3. Fetch rules from database directly without Flow observer overhead
                val rules = regexRuleDao.getAllRulesDirect()

                // Step A: Whitelist (Priority check)
                val whitelistRules = rules.filter { it.type == RuleType.WHITELIST }
                val matchedWhitelist = findMatchedRule(whitelistRules, sanitizedNumber, digitsOnly)
                if (matchedWhitelist != null) {
                    respondToCall(callDetails, allowCallResponse())
                    return@launch
                }

                // Step B: Blacklist
                val blacklistRules = rules.filter { it.type == RuleType.BLACKLIST }
                val matchedBlacklist = findMatchedRule(blacklistRules, sanitizedNumber, digitsOnly)
                if (matchedBlacklist != null) {
                    // Log the block event before responding
                    try {
                        blockLogDao.insertLog(
                            BlockLog(
                                phoneNumber = sanitizedNumber,
                                timestamp = System.currentTimeMillis(),
                                reason = matchedBlacklist.pattern
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to insert block log", e)
                    }

                    respondToCall(callDetails, blockCallResponse())
                    return@launch
                }

                // Step C: Default allow
                respondToCall(callDetails, allowCallResponse())
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in onScreenCall, fallback to allow call", e)
                respondToCall(callDetails, allowCallResponse())
            }
        }
    }

    private fun findMatchedRule(
        rules: List<RegexRule>,
        sanitizedNumber: String,
        digitsOnly: String
    ): RegexRule? {
        for (rule in rules) {
            if (matchesPattern(rule.pattern, sanitizedNumber, digitsOnly)) {
                return rule
            }
        }
        return null
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
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
    }

    companion object {
        private const val TAG = "CallScreeningService"

        /**
         * Safely checks whether the number matches the regex pattern.
         * Handles both full number (with leading +) and digits-only format.
         * Resilient against leading unescaped + meta-character.
         */
        fun matchesPattern(patternString: String, sanitizedNumber: String, digitsOnly: String): Boolean {
            val pattern = try {
                Pattern.compile(patternString)
            } catch (e: PatternSyntaxException) {
                // If user entered +number, escape the leading '+'
                if (patternString.startsWith("+")) {
                    try {
                        Pattern.compile("\\" + patternString)
                    } catch (ignored: Exception) {
                        Log.w(TAG, "Invalid regex pattern: $patternString", ignored)
                        return false
                    }
                } else {
                    Log.w(TAG, "Invalid regex pattern: $patternString", e)
                    return false
                }
            }

            return try {
                pattern.matcher(sanitizedNumber).matches() ||
                pattern.matcher(digitsOnly).matches()
            } catch (e: Exception) {
                Log.w(TAG, "Error matching regex: $patternString", e)
                false
            }
        }
    }
}
