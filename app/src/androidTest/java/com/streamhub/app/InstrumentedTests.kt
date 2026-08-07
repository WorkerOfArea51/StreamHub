package com.streamhub.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentedTests {

    @Test
    fun useAppContext() {
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.streamhub.app", appContext.packageName)
    }
}
