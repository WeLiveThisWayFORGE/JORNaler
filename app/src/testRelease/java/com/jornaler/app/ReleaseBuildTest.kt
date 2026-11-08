package com.jornaler.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseBuildTest {
    @Test
    fun releaseVariantIsNotDebuggableAndKeepsTheAppIdentity() {
        assertFalse(BuildConfig.DEBUG)
        assertEquals("release", BuildConfig.BUILD_TYPE)
        assertEquals("com.jornaler.app", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.3", BuildConfig.VERSION_NAME)
        assertTrue(BuildConfig.VERSION_CODE > 3)
        assertFalse(shouldSeedSamplePages(debuggable = BuildConfig.DEBUG, requested = true))
    }
}
