plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.util.Properties

android {
    namespace = "com.reelgrab.core.network"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // Load EXTRACTION_BASE_URL_{DEBUG,RELEASE} overrides from local.properties so the
    // host can be retargeted without touching VCS. Falls back to sensible defaults:
    //   debug   -> 10.0.2.2:3000  (emulator loopback to the dev Node stub)
    //   release -> placeholder URL (must be overridden before shipping)
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val debugBaseUrl = localProps.getProperty(
        "EXTRACTION_BASE_URL_DEBUG",
        "http://10.0.2.2:3000/",
    )
    val releaseBaseUrl = localProps.getProperty(
        "EXTRACTION_BASE_URL_RELEASE",
        "https://reelgrab-extractor.example.com/",
    )
    val apiKey = localProps.getProperty("EXTRACTION_API_KEY", "")

    buildTypes {
        debug {
            buildConfigField("String", "EXTRACTION_BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField("String", "EXTRACTION_API_KEY", "\"$apiKey\"")
            // BODY-level logging in debug so we can see request/response payloads when a
            // fetch fails (the bug "Couldn't reach the server" was masked because the prior
            // BASIC level dropped the underlying HTTP/host error from logcat).
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_HTTP_BODY_LOGGING", "true")
        }
        release {
            buildConfigField("String", "EXTRACTION_BASE_URL", "\"$releaseBaseUrl\"")
            buildConfigField("String", "EXTRACTION_API_KEY", "\"$apiKey\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_HTTP_BODY_LOGGING", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":domain"))

    implementation(libs.bundles.network)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)
}
