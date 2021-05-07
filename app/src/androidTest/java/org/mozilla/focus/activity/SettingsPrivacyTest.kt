/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.activity.robots.browserScreen
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.TestHelper.createMockResponseFromAsset
import org.mozilla.focus.helpers.TestHelper.exitToTop
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class SettingsPrivacyTest {
    private lateinit var webServer: MockWebServer

    @get: Rule
    var mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = false)

    @Before
    fun setUp() {
        webServer = MockWebServer()
        webServer.enqueue(createMockResponseFromAsset("tracking-cookies.html"))
        webServer.start()
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
        try {
            webServer.close()
            webServer.shutdown()
        } catch (e: IOException) {
            throw AssertionError("Could not stop web server", e)
        }
    }

    @Ignore("Failing due to bug: https://github.com/mozilla-mobile/focus-android/issues/4864")
    @Test
    fun privacyTrackersToggleTest() {
        val trackingPageUrl = webServer.url("").toString()

        searchScreen {
        }.loadPage(trackingPageUrl) {
            verifyPageContent("social tracking blocked")
            verifyPageContent("ads tracking blocked")
            verifyPageContent("analytics tracking blocked")
        /* Go to settings and disable everything */
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            switchAdTrackersToggle()
            switchAnalyticTrackersToggle()
            switchSocialTrackersToggle()
            exitToTop()
        }
        browserScreen {
        }.openMainMenu {
        }.refreshPage {
            verifyPageContent("social tracking not blocked")
            verifyPageContent("ads tracking not blocked")
            verifyPageContent("analytics tracking not blocked")
        }
    }

    @Test
    fun testBlock3rdPartyCookies() {
        val trackingPageUrl = "https://senglehardt.com/test/trackingprotection/test_pages/tracking_protection.html"

        searchScreen {
        }.loadPage(trackingPageUrl) {
            /* Go to settings and disable Trackers Protection for the test to work */
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            switchAdTrackersToggle()
            switchAnalyticTrackersToggle()
            switchSocialTrackersToggle()
            openCookiesSettings()
            verifyDefaultCookiesSettings()
            exitToTop()
        }
        browserScreen {
        }.openMainMenu {
        }.refreshPage {
            verifyPageContent("Cookies BLOCKED")
        }
    }

    @Test
    fun testBlockAllCookies() {
        val trackingPageUrl = "https://senglehardt.com/test/trackingprotection/test_pages/tracking_protection.html"

        searchScreen {
        }.loadPage(trackingPageUrl) {
            /* Go to settings and disable Trackers Protection for the test to work */
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            switchAdTrackersToggle()
            switchAnalyticTrackersToggle()
            switchSocialTrackersToggle()
            openCookiesSettings()
            selectCookieSetting("Yes")
            exitToTop()
        }
        browserScreen {
        }.openMainMenu {
        }.refreshPage {
            verifyPageContent("Cookies BLOCKED")
        }
    }


    @Test("Failing due to bug?: https://github.com/mozilla-mobile/focus-android/issues/4740")
    fun testBlockNoCookies() {
        val trackingPageUrl = "https://senglehardt.com/test/trackingprotection/test_pages/tracking_protection.html"

        searchScreen {
        }.loadPage(trackingPageUrl) {
            /* Go to settings and disable Trackers Protection for the test to work */
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            switchAdTrackersToggle()
            switchAnalyticTrackersToggle()
            switchSocialTrackersToggle()
            openCookiesSettings()
            selectCookieSetting("No")
            exitToTop()
        }
        browserScreen {
        }.openMainMenu {
        }.refreshPage {
            verifyPageContent("Cookies not blocked")
        }
    }
}
