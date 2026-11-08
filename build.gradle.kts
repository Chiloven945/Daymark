buildscript {
    dependencies {
        // AGP 9 ships built-in Kotlin. Pin the Kotlin Gradle plugin so the
        // built-in compiler, Compose compiler, and serialization plugin all
        // use the same Kotlin version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
