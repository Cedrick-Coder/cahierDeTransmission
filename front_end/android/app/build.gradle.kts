import java.io.File

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.front_end"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.smmec.equipeINFO.cahierDeTransmisssion"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// Ensure native dependency SONAMEs are available: duplicate libzkalg12.so -> libzkalg.so
tasks.register("duplicateZkAlg") {
    doLast {
        val jniDir = file("src/main/jniLibs")
        if (jniDir.exists()) {
            jniDir.walkTopDown().filter { it.isFile && it.name == "libzkalg12.so" }.forEach { src ->
                val dest = src.parentFile.toPath().resolve("libzkalg.so").toFile()
                if (!dest.exists()) {
                    src.copyTo(dest, overwrite = true)
                    println("Created ${dest.absolutePath} from ${src.absolutePath}")
                }
            }
        }
    }
}

tasks.named("preBuild") { dependsOn("duplicateZkAlg") }

flutter {
    source = "../.."
}
