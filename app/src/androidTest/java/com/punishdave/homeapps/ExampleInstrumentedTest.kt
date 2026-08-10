package com.punishdave.homeapps

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.punishdave.homeapps.uitest", appContext.packageName)
    }

    @Test
    fun backgroundRefreshCanBeScheduledAndCancelledInIsolatedApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        RefreshScheduler.apply(context, RefreshSettings(enabled = true, backgroundEnabled = true, intervalMinutes = 15))

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(RefreshScheduler.WorkName)
            .get(10, TimeUnit.SECONDS)

        assertTrue(work.isNotEmpty())
        assertTrue(work.single().tags.contains(HomeRefreshWorker::class.java.name))
        WorkManager.getInstance(context).cancelUniqueWork(RefreshScheduler.WorkName).result.get(10, TimeUnit.SECONDS)
    }
}
