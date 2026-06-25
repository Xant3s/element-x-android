/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import io.element.android.features.sharing.api.DirectShareShortcutsPublisher
import io.element.android.features.sharing.api.SharingRoomInfo

class FakeDirectShareShortcutsPublisher : DirectShareShortcutsPublisher {
    override suspend fun publishShortcutsForRooms(rooms: List<SharingRoomInfo>) {}
}
