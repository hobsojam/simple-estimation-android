package com.hobsojam.simpleestimation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaffoldTest {
    @Test
    fun packageNamespaceMatchesApplicationId() {
        assertEquals("com.hobsojam.simpleestimation", BuildConfig.APPLICATION_ID)
    }
}
