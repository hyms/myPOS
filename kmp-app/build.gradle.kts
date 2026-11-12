buildscript {
    repositories {
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlin/kotlin-eap") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.20")
        classpath("com.squareup.sqldelight:gradle-plugin:2.0.1")
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.6.10")
    }
}

allprojects {
    repositories {
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlin/kotlin-eap") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}