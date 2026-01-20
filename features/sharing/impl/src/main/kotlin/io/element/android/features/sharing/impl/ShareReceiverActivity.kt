/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.sharing.impl

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import io.element.android.features.share.api.ShareEntryPoint

/**
 * Lightweight receiver activity for shares.
 *
 * Behavior:
 * - resolve shortcut -> roomId using the prefs mapping the manager writes
 * - extract text or URIs (single or multiple)
 * - try to persist read permission for URIs if possible
 * - forward to main activity / room composer using a simple Intent deep-link with extras:
 *   - EXTRA_TARGET_ROOM_ID
 *   - EXTRA_SHARED_TEXT
 *   - EXTRA_SHARED_URIS (ArrayList<Uri>)
 */
class ShareReceiverActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "sharing_shortcuts_prefs"
        private const val PREF_PREFIX = "shareshortcut.room."
        // These extras must match what MainActivity expects, or how we route.
        const val EXTRA_TARGET_ROOM_ID = "io.element.android.features.sharing.extra.TARGET_ROOM_ID"
        const val EXTRA_SHARED_TEXT = "io.element.android.features.sharing.extra.SHARED_TEXT"
        const val EXTRA_SHARED_URIS = "io.element.android.features.sharing.extra.SHARED_URIS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incoming = intent
        val action = incoming?.action
        val type = incoming?.type

        // Resolve room id from shortcut id if present
        val shortcutId = incoming?.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val roomIdFromExtra = incoming?.getStringExtra("room_id")
        val resolvedRoomId = when {
            !shortcutId.isNullOrEmpty() -> {
                prefs.getString(PREF_PREFIX + shortcutId, null) ?: roomIdFromExtra
            }
            else -> roomIdFromExtra
        }

        if (resolvedRoomId.isNullOrEmpty()) {
            // No room: fall back to opening main UI so the user can pick destination manually.
            // Extract and forward the shared content
            val text = incoming?.getStringExtra(Intent.EXTRA_TEXT)
            val uris = extractUris(incoming)
            openMainApp(text, uris, incoming?.type)
            finish()
            return
        }

        // Extract payload
        when (action) {
            Intent.ACTION_SEND -> {
                if (type?.startsWith("text") == true) {
                    handleSendText(incoming, resolvedRoomId)
                } else {
                    handleSendStream(incoming, resolvedRoomId)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                handleSendMultipleStreams(incoming, resolvedRoomId)
            }
            else -> {
                val text = incoming?.getStringExtra(Intent.EXTRA_TEXT)
                val uris = extractUris(incoming)
                openMainApp(text, uris, incoming?.type)
            }
        }

        // Tell the system we used the shortcut (helps ranking)
        shortcutId?.let { ShortcutManagerCompat.reportShortcutUsed(this, it) }

        finish()
    }

    private fun handleSendText(intent: Intent, roomId: String) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        forwardToComposer(roomId, text, null, intent.type)
    }

    private fun handleSendStream(intent: Intent, roomId: String) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val uris = if (uri != null) arrayListOf(uri) else null
        takePersistablePermissionsIfNeeded(uris)
        forwardToComposer(roomId, null, uris, intent.type)
    }

    private fun handleSendMultipleStreams(intent: Intent, roomId: String) {
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        takePersistablePermissionsIfNeeded(uris)
        forwardToComposer(roomId, null, uris, intent.type)
    }

    private fun extractUris(intent: Intent?): ArrayList<Uri>? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { arrayListOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }
    }

    private fun takePersistablePermissionsIfNeeded(uris: List<Uri>?) {
        try {
            val flags = intent.flags
            if (uris != null && flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: SecurityException) {
                        // Not persistable — ignore
                    }
                }
            }
        } catch (t: Throwable) {
            // Defensive: ignore failures in permission persistence
        }
    }

    private fun forwardToComposer(roomId: String, text: String?, uris: ArrayList<Uri>?, type: String?) {
        val out = Intent(this, Class.forName("io.element.android.x.MainActivity")).apply {
            action = Intent.ACTION_SEND
            this.type = type
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ShareEntryPoint.EXTRA_SHARE_TARGET_ROOM_ID, roomId)
            if (!text.isNullOrEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            if (!uris.isNullOrEmpty()) {
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Propagate ClipData to ensure permissions are passed on
                clipData = intent.clipData
            }
        }
        startActivity(out)
    }

    private fun openMainApp(text: String?, uris: ArrayList<Uri>?, type: String?) {
        val out = Intent(this, Class.forName("io.element.android.x.MainActivity")).apply {
            action = Intent.ACTION_SEND
            this.type = type
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!text.isNullOrEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            if (!uris.isNullOrEmpty()) {
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = intent.clipData
            }
        }
        startActivity(out)
    }
}
