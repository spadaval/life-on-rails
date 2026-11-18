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
        // commonTest runs here, on the JVM (boat: :homecore:testAndroidHostTest), and under Node below.
        withHostTest {}
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "homecore"
        browser {
            commonWebpackConfig { outputFileName = "homecore.js" }
            testTask { enabled = false }
        }
        // The common tests also run under Node (wasmJsNodeTest), so boat needs no headless Chrome. The
        // hook hands Skia's browser-only loader its wasm, which it cannot read under Node otherwise.
        nodejs {
            testTask {
                nodeJsArgs += listOf("--import", file("node/skiko-under-node.mjs").toURI().toString())
            }
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
            // Local dates and times for the header and LoR's replies (DeviceHome's zone).
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        androidMain.dependencies {
            // The phone's own back handlers (Platform.android.kt), as the app used before the split.
            implementation("androidx.activity:activity-compose:1.13.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            // Time zones other than UTC for kotlinx-datetime in the browser and under Node (TimeZones.kt).
            // 2.23.0: the last whose peer range takes the @js-joda/core 3.2.0 kotlinx-datetime 0.8.0 uses.
            implementation(npm("@js-joda/timezone", "2.23.0"))
        }
    }
}

// The lab serves the same font files the app draws with, from fonts/ beside the page.
tasks.named<ProcessResources>("wasmJsProcessResources") {
    from("src/androidMain/res/font") { into("fonts") }
}

// The design concepts (src/webMain/.../concepts/, one package each beside the shared kit/), in the
// order the lab's Version dropdown lists them after `current`. This list is their registration:
// a direction's package holds `object <Name> { val version = Version(...) }`, and the registry the
// lab reads (Concepts.all) is generated from the list. -Plor.concepts=timeline (web-dev.sh's
// --only) compiles only the named concepts and leaves the others' packages out of the source set,
// so the capped local development build holds current, the kit and the concept being worked on
// (lor-j1fe: the build with every concept outgrew the scope's 4 GB). Unset, every concept is in,
// as boat's full bundle needs.
val concepts = listOf("queue" to "Queue", "timeline" to "Timeline", "panes" to "Panes", "context" to "Context")
val conceptsOnly: List<String>? = providers.gradleProperty("lor.concepts").orNull
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
conceptsOnly?.forEach { name ->
    require(concepts.any { it.first == name }) { "lor.concepts: no concept named $name; there are ${concepts.joinToString { it.first }}" }
}
val conceptsIn = concepts.filter { conceptsOnly == null || it.first in conceptsOnly }
val conceptsRegistry = layout.buildDirectory.dir("generated/lor-concepts/kotlin")
val generateConcepts = tasks.register("generateConcepts") {
    val names = conceptsIn.map { it.first to it.second }
    val out = conceptsRegistry
    inputs.property("concepts", names.map { "${it.first}:${it.second}" })
    outputs.dir(out)
    doLast {
        val file = out.get().file("com/spadaval/lor/concepts/Concepts.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "// Generated by homecore/build.gradle.kts from its `concepts` list (lor.concepts: " +
                (conceptsOnly?.joinToString(",") ?: "all") + "). Do not edit.\n" +
                "package com.spadaval.lor.concepts\n\n" +
                "import com.spadaval.lor.lab.Version\n\n" +
                "object Concepts {\n" +
                "    val all: List<Version> = listOf(" +
                names.joinToString { "com.spadaval.lor.concepts.${it.first}.${it.second}.version" } + ")\n" +
                "}\n",
        )
    }
}
// webMain comes from the default hierarchy, which makes it after this script runs.
kotlin.sourceSets.matching { it.name == "webMain" }.configureEach {
    kotlin.srcDir(generateConcepts)
    concepts.filter { it !in conceptsIn }.forEach { kotlin.exclude("com/spadaval/lor/concepts/${it.first}/**") }
}
