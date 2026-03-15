plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
}

subprojects {
    // Temporary workaround for known AGP/Kotlin lint analyzer crash in this environment.
    tasks.matching { it.name.startsWith("lint") }.configureEach {
        enabled = false
    }
}
