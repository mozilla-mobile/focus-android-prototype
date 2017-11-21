/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import org.mozilla.focus.architecture.NonNullLiveData
import org.mozilla.focus.architecture.NonNullMutableLiveData
import org.mozilla.focus.customtabs.CustomTabConfig
import org.mozilla.focus.shortcut.HomeScreen
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.UrlUtils
import java.util.*

/**
 * Sessions are managed by this global SessionManager instance.
 */
class SessionManager private constructor() {

    private val sessions: NonNullMutableLiveData<List<Session>>
    private var currentSessionUUID: String? = null

    /**
     * Get the current session. This method will throw an exception if there's no active session.
     */
    val currentSession: Session
        get() {
            if (currentSessionUUID == null) {
                throw IllegalAccessError("There's no active session")
            }

            return getSessionByUUID(currentSessionUUID!!)
        }

    val numberOfSessions: Int
        get() = sessions.value.size

    val positionOfCurrentSession: Int
        get() {
            if (currentSessionUUID == null) {
                return -1
            }

            for (i in 0 until this.sessions.value.size) {
                val session = this.sessions.value[i]

                if (session.uuid == currentSessionUUID) {
                    return i
                }
            }

            return -1
        }

    init {
        this.sessions = NonNullMutableLiveData(
                Collections.unmodifiableList(emptyList()))
    }

    /**
     * Handle this incoming intent (via onCreate()) and create a new session if required.
     */
    fun handleIntent(context: Context, intent: SafeIntent, savedInstanceState: Bundle?) {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            // This Intent was launched from history (recent apps). Android will redeliver the
            // original Intent (which might be a VIEW intent). However if there's no active browsing
            // session then we do not want to re-process the Intent and potentially re-open a website
            // from a session that the user already "erased".
            return
        }

        if (savedInstanceState != null) {
            // We are restoring a previous session - No need to handle this Intent.
            return
        }

        createSessionFromIntent(context, intent)
    }

    /**
     * Handle this incoming intent (via onNewIntent()) and create a new session if required.
     */
    fun handleNewIntent(context: Context, intent: SafeIntent) {
        createSessionFromIntent(context, intent)
    }

    private fun createSessionFromIntent(context: Context, intent: SafeIntent) {
        val action = intent.action

        if (Intent.ACTION_VIEW == action) {
            val dataString = intent.dataString
            if (TextUtils.isEmpty(dataString)) {
                return  // If there's no URL in the Intent then we can't create a session.
            }

            if (intent.hasExtra(HomeScreen.ADD_TO_HOMESCREEN_TAG)) {
                val blockingEnabled = intent.getBooleanExtra(HomeScreen.BLOCKING_ENABLED, true)
                createSession(context, Source.HOME_SCREEN, intent, intent.dataString, blockingEnabled)
            } else {
                createSession(context, Source.VIEW, intent, intent.dataString)
            }
        } else if (Intent.ACTION_SEND == action) {
            val dataString = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (TextUtils.isEmpty(dataString)) {
                return
            }

            val isSearch = !UrlUtils.isUrl(dataString)

            val url = if (isSearch)
                UrlUtils.createSearchUrl(context, dataString)
            else
                dataString

            if (isSearch) {
                createSearchSession(Source.SHARE, url, dataString)
            } else {
                createSession(Source.SHARE, url)
            }
        }
    }

    /**
     * Is there at least one browsing session?
     */
    fun hasSession(): Boolean {
        return !sessions.value.isEmpty()
    }

    fun isCurrentSession(session: Session): Boolean {
        return session.uuid == currentSessionUUID
    }

    fun hasSessionWithUUID(uuid: String): Boolean {
        for (session in sessions.value) {
            if (uuid == session.uuid) {
                return true
            }
        }

        return false
    }

    fun getSessionByUUID(uuid: String): Session {
        for (session in sessions.value) {
            if (uuid == session.uuid) {
                return session
            }
        }

        throw IllegalAccessError("There's no active session with UUID " + uuid)
    }

    fun getSessions(): NonNullLiveData<List<Session>> {
        return sessions
    }

    fun createSession(source: Source, url: String) {
        val session = Session(source, url)
        addSession(session)
    }

    fun createSearchSession(source: Source, url: String, searchTerms: String) {
        val session = Session(source, url)
        session.searchTerms = searchTerms
        addSession(session)
    }

    private fun createSession(context: Context, source: Source, intent: SafeIntent, url: String?) {
        val session = if (CustomTabConfig.isCustomTabIntent(intent))
            Session(url, CustomTabConfig.parseCustomTabIntent(context, intent))
        else
            Session(source, url)
        addSession(session)
    }

    private fun createSession(context: Context, source: Source, intent: SafeIntent, url: String?, blockingEnabled: Boolean) {
        val session = if (CustomTabConfig.isCustomTabIntent(intent))
            Session(url, CustomTabConfig.parseCustomTabIntent(context, intent))
        else
            Session(source, url)
        session.isBlockingEnabled = blockingEnabled
        addSession(session)
    }

    private fun addSession(session: Session) {
        currentSessionUUID = session.uuid

        val sessions = ArrayList(this.sessions.value)
        sessions.add(session)

        this.sessions.value = Collections.unmodifiableList(sessions)
    }

    fun selectSession(session: Session) {
        if (session.uuid == currentSessionUUID) {
            // This is already the selected session.
            return
        }

        currentSessionUUID = session.uuid

        this.sessions.value = this.sessions.value
    }

    /**
     * Remove all sessions.
     */
    fun removeAllSessions() {
        currentSessionUUID = null

        sessions.value = Collections.unmodifiableList(emptyList())
    }

    /**
     * Remove the current (selected) session.
     */
    fun removeCurrentSession() {
        removeSession(currentSessionUUID)
    }

    @VisibleForTesting internal fun removeSession(uuid: String?) {
        val sessions = ArrayList<Session>()

        var removedFromPosition = -1

        for (i in 0 until this.sessions.value.size) {
            val currentSession = this.sessions.value[i]

            if (currentSession.uuid == uuid) {
                removedFromPosition = i
                continue
            }

            sessions.add(currentSession)
        }

        if (removedFromPosition == -1) {
            return
        }

        if (sessions.isEmpty()) {
            currentSessionUUID = null
        } else {
            val currentSession = sessions[Math.min(removedFromPosition, sessions.size - 1)]
            currentSessionUUID = currentSession.uuid
        }

        this.sessions.value = sessions
    }

    companion object {
        val instance = SessionManager()
    }
}
