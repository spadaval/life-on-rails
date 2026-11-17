import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The code the app and the browser lab share: Compose for Android (the app's dependency) and for
// wasmJs (the lab page, wasmJsMain). Nothing in the browser build talks to LoR or T3.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val composeVersion = "1.12.2"

kotlin {
    android {
        namespace = "com.spadaval.lor.homecore"
        compileSdk = 37
        minSdk = 31
        // The Saira fonts in androidMain/res/font, read as com.spadaval.lor.homecore.R.font.
        androidResources { enable = true }
        // commonTest runs here, on the JVM (boat: :homecore:testAndroidHostTest). Under Node the
        // browser's tests can't start: Compose's Skia (skiko.mjs) fails to load there.
        withHostTest {}
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "homecore"
        browser {
            commonWebpackConfig { outputFileName = "homecore.js" }
            testTask { enabled = false }
        }
        binaries.executable()
    }

    // kotlin.time.Instant, which the panels' state carries.
    compilerOptions { optIn.add("kotlin.time.ExperimentalTime") }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
            implementation("org.jetbrains.compose.ui:ui:$composeVersion")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            // Back as Compose's common code hears it (NavigationBackHandler); the lab's Back button
            // and gesture.mjs's back() send it through a DirectNavigationEventInput.
            implementation("org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0")
            // api: the app's debug fixture entry reads a fixture's JSON parts too.
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            // Local days and clock times for the panels' rows (lor-rgx4); already in the wasm build
            // through Compose. Named zones come from the platform (LocalClock.kt), not from a zone
            // database: the browser build would need @js-joda/timezone for that.
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
    }
}

// The lab serves the same font files the app draws with, from fonts/ beside the page.
tasks.named<ProcessResources>("wasmJsProcessResources") {
    from("src/androidMain/res/font") { into("fonts") }
}
