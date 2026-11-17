plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    // homecore: the code the app and the browser lab share (Kotlin Multiplatform, Android and wasmJs).
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("com.android.kotlin.multiplatform.library") version "9.2.1" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    // homecore's fixtures (lor-oech) read and write JSON: a state in the lab's URL, and the layout file and LoR's payloads they carry.
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
}

// homecore's browser build downloads Node.js, Yarn and Binaryen. By default the Kotlin plugin adds
// their sites as project repositories, which settings.gradle.kts refuses (FAIL_ON_PROJECT_REPOS);
// with no download URL it adds none, and the settings' own repositories serve them.
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
fun Project.noToolRepositories() {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}
allprojects { noToolRepositories() }
