package com.jornaler.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseBehaviorTest {
    @Test
    fun releaseBuildDoesNotSeedSamplePages() {
        assertFalse(shouldSeedSamplePages(debuggable = false, requested = true))
        assertFalse(shouldSeedSamplePages(debuggable = false, requested = false))
        assertFalse(shouldSeedSamplePages(debuggable = true, requested = false))
        assertTrue(shouldSeedSamplePages(debuggable = true, requested = true))
    }
}
