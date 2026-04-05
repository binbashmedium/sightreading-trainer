plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    // Paparazzi is applied below conditionally so the plugin's task-level modifications
    // (bytecode instrumentation, custom test reporter, preparePaparazziDebugResources dep)
    // don't interfere with standard unit tests when running :app:test.
    // Pass -PskipPaparazziPlugin=true to run unit tests without the plugin overhead.
    // The screenshots job omits this flag and calls recordPaparazziDebug directly.
}

android {
    namespace = "com.binbashmedium.sightreadingtrainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.binbashmedium.sightreadingtrainer"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    sourceSets {
        // ScreenshotTest.kt lives in src/screenshotTest/kotlin and is only added to the
        // test compilation classpath when the Paparazzi plugin is applied (screenshots CI
        // step). When -PskipPaparazziPlugin=true (unit-test CI step) the directory is not
        // included, so no Paparazzi types are needed on the classpath.
        named("test") {
            if (project.findProperty("skipPaparazziPlugin") != "true") {
                java.srcDir("src/screenshotTest/kotlin")
            }
        }
    }

    testOptions {
        unitTests {
            // When the Paparazzi plugin IS applied (screenshots CI step), still exclude
            // ScreenshotTest from the standard :app:test task; it only runs via
            // recordPaparazziDebug / verifyPaparazziDebug.
            all { testTask ->
                testTask.exclude("**/ScreenshotTest.class")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Apply Paparazzi plugin (registers recordPaparazziDebug / verifyPaparazziDebug).
// When -PskipPaparazziPlugin=true is passed (e.g. in the unit-test CI step) the plugin
// is not applied so its bytecode instrumentation and custom test reporter don't interfere
// with :app:test. ScreenshotTest.kt also lives in a separate src/screenshotTest/kotlin
// source directory that is only added to the test classpath when the plugin IS applied
// (see sourceSets above), so no Paparazzi types are needed when running unit tests.
if (project.findProperty("skipPaparazziPlugin") != "true") {
    apply(plugin = "app.cash.paparazzi")
}

dependencies {
    implementation(project(":domain"))

    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
