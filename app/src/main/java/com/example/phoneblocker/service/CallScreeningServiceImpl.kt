package com.example.phoneblocker.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.phoneblocker.data.local.AppDatabase
import com.example.phoneblocker.data.local.entity.BlockLog
import com.example.phoneblocker.data.local.entity.RegexRule
import com.example.phoneblocker.data.local.entity.RuleType
import com.example.phoneblocker.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class CallScreeningServiceImpl : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        // Only screen incoming calls; never block outgoing calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            safeRespondToCall(callDetails, allowCallResponse())
            return
        }

        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val repository = UserPreferencesRepository(context)
        val blockLogDao = database.blockLogDao()
        val regexRuleDao = database.regexRuleDao()

        serviceScope.launch {
            try {
                // Enforce a 3-second hard timeout using withTimeoutOrNull to prevent TimeoutCancellationException swallowing
                val isHandled = withTimeoutOrNull(3000L) {
                    // Master Switch Check
                    val isBlockingEnabled = repository.isBlockingEnabled.firstOrNull() ?: false
                    if (!isBlockingEnabled) {
                        safeRespondToCall(callDetails, allowCallResponse())
                        return@withTimeoutOrNull true
                    }

                    // 2. Phone Number Sanitization & Multi-format Normalization
                    val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
                    val sanitizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                    val digitsOnly = sanitizedNumber.removePrefix("+")
                    val localNumber = digitsOnly.removePrefix("86")
                    // Restore national trunk prefix '0' for domestic landlines (e.g., +8610... -> 010..., +8621... -> 021...)
                    val nationalLandline = restoreNationalLandline(localNumber)

                    // Handle private/unknown/restricted caller
                    if (sanitizedNumber.isBlank()) {
                        val blockUnknown = repository.isBlockUnknownEnabled.firstOrNull() ?: false
                        if (blockUnknown) {
                            safeRespondToCall(callDetails, blockCallResponse())
                            logBlockEvent(blockLogDao, "Private / Unknown Number", "UNKNOWN_CALLER")
                        } else {
                            safeRespondToCall(callDetails, allowCallResponse())
                        }
                        return@withTimeoutOrNull true
                    }

                    // 3. Fetch rules from database directly without Flow observer overhead
                    val rules = regexRuleDao.getAllRulesDirect()

                    // Step A: Whitelist (Priority check)
                    val whitelistRules = rules.filter { it.type == RuleType.WHITELIST }
                    if (findMatchedRule(whitelistRules, sanitizedNumber, digitsOnly, localNumber, nationalLandline) != null) {
                        safeRespondToCall(callDetails, allowCallResponse())
                        return@withTimeoutOrNull true
                    }

                    // Step B: Blacklist
                    val blacklistRules = rules.filter { it.type == RuleType.BLACKLIST }
                    val matchedBlacklist = findMatchedRule(blacklistRules, sanitizedNumber, digitsOnly, localNumber, nationalLandline)
                    if (matchedBlacklist != null) {
                        // Respond immediately to eliminate "first ring glitch"!
                        safeRespondToCall(callDetails, blockCallResponse())

                        // Persist log asynchronously without blocking the decision
                        logBlockEvent(blockLogDao, sanitizedNumber, matchedBlacklist.pattern)
                        return@withTimeoutOrNull true
                    }

                    // Step C: Default allow
                    safeRespondToCall(callDetails, allowCallResponse())
                    true
                }

                // Handle 3-second timeout fallback
                if (isHandled == null) {
                    Log.w(TAG, "Screening timed out after 3000ms, fallback to allow call")
                    safeRespondToCall(callDetails, allowCallResponse())
                }
            } catch (e: Exception) {
                // Avoid calling into dead binder if service was unbound
                if (e !is CancellationException) {
                    Log.e(TAG, "Unexpected error in onScreenCall, fallback to allow call", e)
                    safeRespondToCall(callDetails, allowCallResponse())
                }
            }
        }
    }

    private fun safeRespondToCall(callDetails: Call.Details, response: CallResponse) {
        try {
            respondToCall(callDetails, response)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to respondToCall (Service may be unbinding)", e)
        }
    }

    private fun logBlockEvent(blockLogDao: com.example.phoneblocker.data.local.dao.BlockLogDao, number: String, reason: String) {
        serviceScope.launch {
            try {
                blockLogDao.insertLog(
                    BlockLog(
                        phoneNumber = number,
                        timestamp = System.currentTimeMillis(),
                        reason = reason
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert block log", e)
            }
        }
    }

    private fun findMatchedRule(
        rules: List<RegexRule>,
        sanitizedNumber: String,
        digitsOnly: String,
        localNumber: String,
        nationalLandline: String
    ): RegexRule? {
        for (rule in rules) {
            if (matchesPattern(rule.pattern, sanitizedNumber, digitsOnly, localNumber, nationalLandline)) {
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
        private val patternCache = ConcurrentHashMap<String, Pattern?>()

        fun clearPatternCache() {
            patternCache.clear()
        }

        fun restoreNationalLandline(localNumber: String): String {
            return if (localNumber.isNotEmpty() && !localNumber.startsWith("0")) {
                // Beijing landline is 10-xxxxxxx, other landlines start with 2-9.
                // Mobile numbers start with 1[3-9].
                if (localNumber.startsWith("10") || !localNumber.startsWith("1")) {
                    "0$localNumber"
                } else {
                    localNumber
                }
            } else {
                localNumber
            }
        }

        /**
         * Safely checks whether the number matches the regex pattern.
         * Handles raw number (with '+'), digits-only, country-code-stripped (+86),
         * and landline-restored numbers.
         */
        fun matchesPattern(
            patternString: String,
            sanitizedNumber: String,
            digitsOnly: String,
            localNumber: String = digitsOnly.removePrefix("86"),
            nationalLandline: String = restoreNationalLandline(localNumber)
        ): Boolean {
            val cleanPattern = patternString.trim()
            if (cleanPattern.isBlank()) return false

            val pattern = patternCache.computeIfAbsent(cleanPattern) { raw ->
                try {
                    val safeRegex = when {
                        raw.startsWith("+") -> "\\" + raw
                        // Avoid escaping bracketed character classes like ^[+0-9]
                        raw.startsWith("^+") && !raw.startsWith("^[+") -> "^\\+" + raw.removePrefix("^+")
                        else -> raw
                    }
                    Pattern.compile(safeRegex)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid pattern skipped: $raw", e)
                    null
                }
            } ?: return false

            return try {
                pattern.matcher(sanitizedNumber).matches() ||
                pattern.matcher(digitsOnly).matches() ||
                pattern.matcher(localNumber).matches() ||
                pattern.matcher(nationalLandline).matches()
            } catch (e: Exception) {
                Log.w(TAG, "Error matching regex: $cleanPattern", e)
                false
            }
        }
    }
}
