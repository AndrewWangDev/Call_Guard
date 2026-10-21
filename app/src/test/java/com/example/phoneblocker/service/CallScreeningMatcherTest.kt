package com.example.phoneblocker.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallScreeningMatcherTest {

    @Test
    fun testStandardRegexMatch() {
        // Regex pattern: ^1\d{10}$ (typical 11-digit mobile number)
        val pattern = "^1\\d{10}$"
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "13800138000", "13800138000"))
        // Incoming call with +86
        assertTrue(CallScreeningServiceImpl.matchesPattern("^861\\d{10}$", "+8613800138000", "8613800138000"))
    }

    @Test
    fun testInternationalPlusHandling() {
        // Number with leading '+'
        val rawNumber = "+8613800138000"
        val digitsOnly = "8613800138000"

        // Rule with explicit \+
        assertTrue(CallScreeningServiceImpl.matchesPattern("^\\+86.*", rawNumber, digitsOnly))

        // Rule with unescaped leading + (should not crash with PatternSyntaxException)
        assertTrue(CallScreeningServiceImpl.matchesPattern("+8613800138000", rawNumber, digitsOnly))

        // Rule without + matching digitsOnly
        assertTrue(CallScreeningServiceImpl.matchesPattern("^86138.*", rawNumber, digitsOnly))
    }

    @Test
    fun testMalformedRegexDoesNotCrash() {
        val number = "13800138000"
        // Unclosed bracket
        assertFalse(CallScreeningServiceImpl.matchesPattern("[0-9", number, number))
        // Dangling quantifier
        assertFalse(CallScreeningServiceImpl.matchesPattern("*123", number, number))
        // Unclosed parenthesis
        assertFalse(CallScreeningServiceImpl.matchesPattern("(123", number, number))
    }

    @Test
    fun testPrefixAndWildcards() {
        val number = "4008001234"
        assertTrue(CallScreeningServiceImpl.matchesPattern("^400.*", number, number))
        assertFalse(CallScreeningServiceImpl.matchesPattern("^800.*", number, number))
    }
}
