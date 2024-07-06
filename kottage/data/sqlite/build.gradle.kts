import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.buildlogic.multiplatform.library)
    alias(libs.plugins.buildlogic.android.library)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("KottageDatabase") {
            packageName = "io.github.irgaly.kottage.data.sqlite"
        }
    }
}

android {
    namespace = "io.github.irgaly.kottage.data.sqlite"
}

kotlin {
    // JS
    js(IR) {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }
    mingwX64 {
        binaries.configureEach {
            linkerOpts("-LC:/msys64/mingw64/lib", "-lsqlite3")
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.kottage.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.kottage.core.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.android)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.jvm)
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(libs.sqldelight.driver.native)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("better-sqlite3", "9.2.2"))
                //implementation(npm("@types/better-sqlite3", "9.2.2", generateExternals = true))
            }
        }
    }
    if (providers.environmentVariable("GITHUB_ACTIONS").isPresent
        && OperatingSystem.current().isLinux
    ) {
        targets.withType<KotlinNativeTarget> {
            if ("linux" in name) {
                binaries.all {
                    linkerOpts.add("-L/usr/lib/x86_64-linux-gnu")
                }
            }
        }
    }
}

