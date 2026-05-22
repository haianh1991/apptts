package com.example.webreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckResolverTest {

    @Test
    fun testNoUpdateNeeded() {
        val currentVersionCode = 10
        val info = AppUpdateInfo(
            latestVersionCode = 10,
            versionName = "1.0.0",
            minVersionCode = 10,
            updateUrl = "https://example.com/update",
            releaseNotes = "No update needed"
        )
        val result = UpdateCheckResolver.resolve(currentVersionCode, info)
        assertFalse("Should not show update dialog", result.showDialog)
        assertFalse("Should not force update", result.isForce)
    }

    @Test
    fun testNoUpdateNeededWithOlderServerVersion() {
        val currentVersionCode = 10
        val info = AppUpdateInfo(
            latestVersionCode = 9,
            versionName = "0.9.0",
            minVersionCode = 8,
            updateUrl = "https://example.com/update",
            releaseNotes = "Older version on server"
        )
        val result = UpdateCheckResolver.resolve(currentVersionCode, info)
        assertFalse("Should not show update dialog", result.showDialog)
        assertFalse("Should not force update", result.isForce)
    }

    @Test
    fun testOptionalUpdate() {
        val currentVersionCode = 10
        val info = AppUpdateInfo(
            latestVersionCode = 11,
            versionName = "1.0.1",
            minVersionCode = 10,
            updateUrl = "https://example.com/update",
            releaseNotes = "Optional bug fix"
        )
        val result = UpdateCheckResolver.resolve(currentVersionCode, info)
        assertTrue("Should show update dialog", result.showDialog)
        assertFalse("Should not force update", result.isForce)
    }

    @Test
    fun testForceUpdate() {
        val currentVersionCode = 10
        val info = AppUpdateInfo(
            latestVersionCode = 12,
            versionName = "1.1.0",
            minVersionCode = 11,
            updateUrl = "https://example.com/update",
            releaseNotes = "Critical security update"
        )
        val result = UpdateCheckResolver.resolve(currentVersionCode, info)
        assertTrue("Should show update dialog", result.showDialog)
        assertTrue("Should force update", result.isForce)
    }
}
