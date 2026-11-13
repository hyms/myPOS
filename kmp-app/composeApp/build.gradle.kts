plugins {
    id("com.android.application") version "8.6.1"
    id("org.jetbrains.kotlin.multiplatform") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("org.jetbrains.compose") version "1.6.10"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.compose.runtime:runtime:1.6.10")
                implementation("org.jetbrains.compose.foundation:foundation:1.6.10")
                implementation("org.jetbrains.compose.material3:material3:1.6.10")
                implementation("org.jetbrains.compose.material3:material3-adaptive:1.6.10")
                implementation("org.jetbrains.compose.ui:ui:1.6.10")
                implementation("io.insert-koin:koin-core-compose:4.0.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.0.20")
                implementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.20")
                implementation("org.jetbrains.compose.ui:ui-test-junit4:1.6.10")
                implementation("org.jetbrains.compose.ui:ui-test-manifest:1.6.10")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
                implementation("org.junit.jupiter:junit-jupiter:5.11.3")
                implementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
                implementation("app.cash.turbine:turbine:1.0.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.ui:ui:1.6.10")
                implementation("org.jetbrains.compose.ui:ui-test-junit4:1.6.10")
                implementation("org.jetbrains.compose.ui:ui-test-manifest:1.6.10")
            }
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "com.tpvmovilexpress.composeapp"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}