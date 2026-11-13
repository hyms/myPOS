plugins {
    id("com.android.library") version "8.6.1"
    id("org.jetbrains.kotlin.multiplatform") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    jvm()
    android()

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val targets = listOf(
        iosX64("iosX64"),
        iosArm64("iosArm64"),
        iosSimulatorArm64("iosSimulatorArm64")
    )
    targets.forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = isMingwX64
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                implementation("com.squareup.okio:okio:3.9.0")
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                implementation("io.insert-koin:koin-core:4.0.0")
                api("io.insert-koin:koin-core-compose:4.0.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.0.20")
                implementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.20")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
                implementation("org.junit.jupiter:junit-jupiter:5.11.3")
                implementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
                implementation("app.cash.turbine:turbine:1.0.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("TPVDatabase") {
            packageName = "com.tpvmovilexpress.data.database"
        }
    }
}

android {
    namespace = "com.tpvmovilexpress.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xexplicit-api=strict"
        freeCompilerArgs += "-Xlint:all"
        freeCompilerArgs += "-Werror"
    }
}