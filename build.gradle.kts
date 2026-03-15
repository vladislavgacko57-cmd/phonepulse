plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

subprojects {
    // Temporary workaround for known AGP/Kotlin lint analyzer crash in this environment.
    tasks.matching { it.name.startsWith("lint") }.configureEach {
        enabled = false
    }
}
