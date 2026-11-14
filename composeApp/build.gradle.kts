buildscript {
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    classpath("org.jetbrains.compose:compose-gradle-plugin:1.7.1")
    classpath("app.cash.sqldelight:gradle-plugin:2.0.1")
    classpath("io.insert-koin:koin-gradle-plugin:3.5.3")
  }
}

apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "org.jetbrains.compose")
apply(plugin = "app.cash.sqldelight")
apply(plugin = "io.insert-koin")

kotlin {
  androidTarget {
    publishLibraryVariants("release")
    publishLibraryVariants("debug")
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.framework)
        implementation(libs.compose.material)
        implementation(libs.sqldelight.runtime)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(compose.ui.tooling.preview)
        implementation(libs.sqldelight.runtime) // Android sqlite driver
      }
    }
    val desktopMain by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.sqldelight.runtime) // Desktop sqlite driver
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin.test)
        implementation(kotlin.test.junit)
        implementation(libs.koin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}

sqldelight {
  database("TpvDatabase") {
    packageName = "com.copitoB.mypos.data.repository"
    sourceFolders = listOf(project.file("src/commonMain/sqldelight"))
  }
}

android {
  namespace = "com.copitoB.mypos"
  defaultConfig {
    applicationId = "com.copitoB.mypos"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"
  }
  compileSdk = 34
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
