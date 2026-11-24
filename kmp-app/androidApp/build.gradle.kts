plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.copitoB.mypos.android"

    compileSdk {
        version = release(libs.versions.android.compile.sdk.get().toInt())
    }

    defaultConfig {
        applicationId = "com.copitoB.mypos"
        minSdk {
            version = release(libs.versions.android.min.sdk.get().toInt())
        }
        targetSdk {
            version = release(libs.versions.android.compile.sdk.get().toInt())
        }
        versionCode = 1
        versionName = "1.0.1"
    }
}

dependencies {
    implementation(project(":composeApp"))
}
