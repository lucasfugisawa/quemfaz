plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("android")
    id("org.jmailen.kotlinter")
}

// No kotlin { ... } block here if it's not a KMP module

android {
    namespace = "com.fugisawa.quemfaz"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.fugisawa.quemfaz"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Set up adb reverse port forwarding so physical devices can reach the local backend.
// This runs automatically before each debug install — no manual `adb reverse` needed.
abstract class AdbReverseTask : DefaultTask() {
    @get:Input abstract val adb: Property<String>
    @get:Input abstract val port: Property<Int>

    @TaskAction
    fun run() {
        val adbPath = adb.get()
        val serverPort = port.get()

        val devicesProc = ProcessBuilder(adbPath, "devices")
            .redirectErrorStream(true).start()
        val devicesOutput = devicesProc.inputStream.bufferedReader().readText()
        devicesProc.waitFor()

        val serials = devicesOutput.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.endsWith("device") }
            .map { it.substringBefore("\t") }
            .toList()

        for (serial in serials) {
            val proc = ProcessBuilder(adbPath, "-s", serial, "reverse", "tcp:$serverPort", "tcp:$serverPort")
                .redirectErrorStream(true).start()
            proc.waitFor()
            println("adb reverse tcp:$serverPort on $serial")
        }
        if (serials.isEmpty()) {
            println("WARNING: No connected devices found — skipping adb reverse")
        }
    }
}

tasks.register<AdbReverseTask>("adbReverseLocalServer") {
    adb.set(android.adbExecutable.absolutePath)
    port.set(8080)
}

afterEvaluate {
    tasks.matching { it.name.startsWith("install") && it.name.contains("Debug") }.configureEach {
        dependsOn("adbReverseLocalServer")
    }
}
