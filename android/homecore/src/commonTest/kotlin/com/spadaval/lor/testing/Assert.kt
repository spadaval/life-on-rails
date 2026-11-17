package com.spadaval.lor.testing

import kotlin.math.abs

/*
 * JUnit 4's assertions in its own argument order (the message first), over kotlin.test, so the
 * home tests that moved here from the app (lor-xdoi) changed only their imports.
 */

fun assertEquals(expected: Any?, actual: Any?) = kotlin.test.assertEquals(expected, actual)

fun assertEquals(message: String?, expected: Any?, actual: Any?) = kotlin.test.assertEquals(expected, actual, message)

fun assertEquals(expected: Double, actual: Double, delta: Double) = assertEquals(null, expected, actual, delta)

fun assertEquals(message: String?, expected: Double, actual: Double, delta: Double) {
    if (expected.compareTo(actual) == 0) return
    kotlin.test.assertTrue(abs(expected - actual) <= delta, (message?.let { "$it: " } ?: "") + "expected:<$expected> but was:<$actual>")
}

fun assertEquals(expected: Float, actual: Float, delta: Float) = assertEquals(null, expected.toDouble(), actual.toDouble(), delta.toDouble())

fun assertEquals(message: String?, expected: Float, actual: Float, delta: Float) = assertEquals(message, expected.toDouble(), actual.toDouble(), delta.toDouble())

fun assertTrue(condition: Boolean) = kotlin.test.assertTrue(condition)

fun assertTrue(message: String?, condition: Boolean) = kotlin.test.assertTrue(condition, message)

fun assertFalse(condition: Boolean) = kotlin.test.assertFalse(condition)

fun assertFalse(message: String?, condition: Boolean) = kotlin.test.assertFalse(condition, message)

fun assertNull(actual: Any?) = kotlin.test.assertNull(actual)

fun assertNull(message: String?, actual: Any?) = kotlin.test.assertNull(actual, message)

fun assertNotNull(actual: Any?) {
    kotlin.test.assertNotNull(actual)
}

fun assertNotNull(message: String?, actual: Any?) {
    kotlin.test.assertNotNull(actual, message)
}

fun assertSame(expected: Any?, actual: Any?) = kotlin.test.assertSame(expected, actual)

fun assertSame(message: String?, expected: Any?, actual: Any?) = kotlin.test.assertSame(expected, actual, message)

fun assertNotSame(expected: Any?, actual: Any?) = kotlin.test.assertNotSame(expected, actual)

fun assertNotEquals(unexpected: Any?, actual: Any?) = kotlin.test.assertNotEquals(unexpected, actual)

fun assertNotEquals(message: String?, unexpected: Any?, actual: Any?) = kotlin.test.assertNotEquals(unexpected, actual, message)

fun fail(): Nothing = kotlin.test.fail()

fun fail(message: String?): Nothing = kotlin.test.fail(message)
