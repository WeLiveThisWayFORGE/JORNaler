plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Release signing reads the environment only. Do not put a path, alias, or
// password in this file. Set all three of JORNALER_KEYSTORE_PATH,
// JORNALER_KEYSTORE_PASSWORD, and JORNALER_KEY_ALIAS to sign. JORNALER_KEY_PASSWORD
// is optional; when it is unset the keystore password is used for the key.
// When none of those variables are set, the release APK stays unsigned.
// A partial set fails the build. Release never uses the debug keystore.
val jornalerKeystorePath = System.getenv("JORNALER_KEYSTORE_PATH").orEmpty().trim()
val jornalerKeystorePassword = System.getenv("JORNALER_KEYSTORE_PASSWORD").orEmpty()
val jornalerKeyAlias = System.getenv("JORNALER_KEY_ALIAS").orEmpty().trim()
val jornalerKeyPasswordOverride = System.getenv("JORNALER_KEY_PASSWORD").orEmpty()
val jornalerReleaseSigningRequired = listOf(
    "JORNALER_KEYSTORE_PATH" to jornalerKeystorePath,
    "JORNALER_KEYSTORE_PASSWORD" to jornalerKeystorePassword,
    "JORNALER_KEY_ALIAS" to jornalerKeyAlias,
)
val jornalerReleaseSigningProvided = jornalerReleaseSigningRequired.any { it.second.isNotBlank() } ||
    jornalerKeyPasswordOverride.isNotBlank()
val jornalerReleaseSigningReady = jornalerReleaseSigningRequired.all { it.second.isNotBlank() }
if (jornalerReleaseSigningProvided && !jornalerReleaseSigningReady) {
    val missing = jornalerReleaseSigningRequired.filter { it.second.isBlank() }.joinToString(", ") { it.first }
    error(
        "Release signing environment is incomplete (missing $missing). " +
            "Set JORNALER_KEYSTORE_PATH, JORNALER_KEYSTORE_PASSWORD, and JORNALER_KEY_ALIAS together, " +
            "or unset every JORNALER_* signing variable to build an unsigned release APK. " +
            "Release builds do not fall back to the debug keystore.",
    )
}

android {
    namespace = "com.jornaler.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jornaler.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (jornalerReleaseSigningReady) {
        signingConfigs {
            create("release") {
                storeFile = file(jornalerKeystorePath)
                storePassword = jornalerKeystorePassword
                keyAlias = jornalerKeyAlias
                keyPassword = jornalerKeyPasswordOverride.ifBlank { jornalerKeystorePassword }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = if (jornalerReleaseSigningReady) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// AGP only creates unit tests for the tested build type (debug) unless a
// release variant asks for them. Release behavior is checked in that variant.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        (variantBuilder as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    implementation("androidx.glance:glance-appwidget:1.2.0")
    // Glance schedules widget work through WorkManager. Pin a current release so
    // targetSdk 37 gets the foreground-service type it requires.
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

logger.lifecycle(
    if (jornalerReleaseSigningReady) {
        "JORNaler release signing: configured from the environment."
    } else {
        "JORNaler release signing: unsigned. No signing environment is set, and the debug keystore is not used."
    },
)

gradle.taskGraph.whenReady {
    val release = extensions.getByType(com.android.build.api.dsl.ApplicationExtension::class.java)
        .buildTypes
        .getByName("release")
    check(!release.isDebuggable) { "Release build type must not be debuggable." }
    val signing = release.signingConfig
    if (!jornalerReleaseSigningReady) {
        check(signing == null) {
            "Release signing was set without JORNALER_KEYSTORE_PATH, JORNALER_KEYSTORE_PASSWORD, and JORNALER_KEY_ALIAS."
        }
    } else {
        check(signing != null && signing.name != "debug") {
            "Release builds do not fall back to the debug signing config."
        }
        val store = signing!!.storeFile?.absoluteFile
        val debugStore = file("${System.getProperty("user.home")}/.android/debug.keystore").absoluteFile
        check(store != null && store != debugStore) {
            "Release signing must not use the Android debug keystore."
        }
    }
}
