/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

import extension.setupAnvil

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.sharing.impl"
}

setupAnvil()

dependencies {
    api(project(":features:sharing:api"))
    implementation(projects.features.share.api)

    implementation(projects.libraries.architecture)
    implementation(projects.libraries.di)
    implementation(projects.libraries.core)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.designsystem) // For tokens/theme if needed, or coil wrappers
    
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat) // For ShareReceiverActivity extending ComponentActivity? No, ComponentActivity is in activity-compose
    implementation(libs.androidx.activity.compose)
    
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
}
