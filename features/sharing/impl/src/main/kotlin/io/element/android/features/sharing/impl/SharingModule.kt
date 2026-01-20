/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.sharing.impl

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import io.element.android.features.sharing.api.SharingShortcutsManager
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn

@Module
@ContributesTo(AppScope::class)
interface SharingModule {
    @Binds
    @SingleIn(AppScope::class)
    fun bindSharingShortcutsManager(impl: DefaultSharingShortcutsManager): SharingShortcutsManager
}
