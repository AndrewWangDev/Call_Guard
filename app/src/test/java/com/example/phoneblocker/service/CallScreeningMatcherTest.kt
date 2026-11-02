package com.example.phoneblocker.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallScreeningMatcherTest {

    @Before
    fun setUp() {
        CallScreeningServiceImpl.clearPatternCache()
    }

    @Test
    fun testStandardRegexMatch() {
        // Regex pattern: ^1\d{10}$ (typical 11-digit mobile number)
        val pattern = "^1\\d{10}$"
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "13800138000", "13800138000"))
        // Incoming call with +86 and plain 86 prefix matching domestic rule via normalization
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "+8613800138000", "8613800138000"))
    }

    @Test
    fun testInternationalPlusHandling() {
        val rawNumber = "+8613800138000"
        val digitsOnly = "8613800138000"

        // Rule with explicit escaped \+
        assertTrue(CallScreeningServiceImpl.matchesPattern("^\\+86.*", rawNumber, digitsOnly))

        // Rule with unescaped leading +
        assertTrue(CallScreeningServiceImpl.matchesPattern("+8613800138000", rawNumber, digitsOnly))

        // Rule with anchored ^+
        assertTrue(CallScreeningServiceImpl.matchesPattern("^+86.*", rawNumber, digitsOnly))

        // Rule without + matching digitsOnly
        assertTrue(CallScreeningServiceImpl.matchesPattern("^86138.*", rawNumber, digitsOnly))
    }

    @Test
    fun testDomesticLandlineNormalization() {
        // Beijing landline rule: starts with 010
        val beijingRule = "^010.*"
        // Delivered as +861088888888
        val rawNumber = "+861088888888"
        val digitsOnly = "861088888888"
        assertTrue(CallScreeningServiceImpl.matchesPattern(beijingRule, rawNumber, digitsOnly))

        // Shanghai landline rule: starts with 021
        val shanghaiRule = "^021\\d{8}$"
        val shRaw = "+862112345678"
        val shDigits = "862112345678"
        assertTrue(CallScreeningServiceImpl.matchesPattern(shanghaiRule, shRaw, shDigits))
    }

    @Test
    fun testCharacterClassStartingWithPlus() {
        // Pattern starting with bracketed character class containing +
        val pattern = "^[+0-9]{5,}$"
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "+8613800138000", "8613800138000"))
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "13800138000", "13800138000"))
    }

    @Test
    fun testCountryCodeNormalization() {
        // User configures standard 11-digit mobile number rule
        val rule = "^138\\d{8}$"

        // Carrier delivers with international prefix +86
        assertTrue(CallScreeningServiceImpl.matchesPattern(rule, "+8613812345678", "8613812345678"))

        // Carrier delivers with local 11 digits
        assertTrue(CallScreeningServiceImpl.matchesPattern(rule, "13812345678", "13812345678"))

        // Unrelated number should not match
        assertFalse(CallScreeningServiceImpl.matchesPattern(rule, "+8615912345678", "8615912345678"))
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

    @Test
    fun testPatternCacheClearing() {
        val pattern = "^1\\d{10}$"
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "13800138000", "13800138000"))
        CallScreeningServiceImpl.clearPatternCache()
        assertTrue(CallScreeningServiceImpl.matchesPattern(pattern, "13800138000", "13800138000"))
    }
}
