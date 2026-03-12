import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

plugins {
    alias(libs.plugins.android.application)
    // mention the kotlin plugin for build purposes (see note below), but don't apply it to app that is not using Kotlin
    alias(libs.plugins.kotlin.android) apply false
}

// required to workaround build issue on CI (duplicate class kotlin...) when building non-Kotlin sample apps
// all apps that use Kotlin have 'kotlin-android' plugin applied already, but plain-Java apps need this workaround to be built on CI
// KotlinAndroidPluginWrapper is translated into plugin with id == "org.jetbrains.kotlin.android" as defined here:
// https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin/build.gradle.kts
apply<KotlinAndroidPluginWrapper>()

android {
    namespace = "com.solarsnap.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.solarsnap.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildToolsVersion = "36.0.0"
    }

    signingConfigs {
        create("release") {
            // You'll configure this through Android Studio UI
            // or manually add keystore details here
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Production optimizations - CRITICAL for avoiding virus detection
            isDebuggable = false
            isJniDebuggable = false
            
            // Remove debug info that triggers false positives
            ndk {
                debugSymbolLevel = "NONE"
            }
            
            // Signing configuration
            signingConfig = signingConfigs.getByName("release")
        }
    }

    repositories {
        // default path where thermalsdk AAR is stored (for CI build)
        flatDir {
            dirs = setOf(File("../../../modules/thermalsdk/build/outputs/aar"))
        }
        // default path where androidsdk AAR is stored (for CI build)
        flatDir {
            dirs = setOf(File("../../../modules/androidsdk/build/outputs/aar"))
        }
        flatDir {
            dirs = setOf(File("libs"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    /**
     * Static configuration for packaging and ABI splitting.
     *
     * This configuration:
     * 1. Sets useLegacyPackaging to true - keeps the traditional APK structure which
     *    may result in more compatibility with older Android versions
     * 2. Enables ABI splitting with only arm64-v8a architecture - produces smaller APKs
     *    by including only 64-bit ARM binary code (most modern Android devices)
     */
    // Configure packaging options with static true value for useLegacyPackaging
    packaging {
        jniLibs {
            // Always use legacy packaging regardless of project properties
            useLegacyPackaging = true
        }
        dex {
            // Always use legacy packaging regardless of project properties
            useLegacyPackaging = true
        }
    }
    // Configure ABI splitting with static values
    splits {
        abi {
            // Always enable ABI splitting
            isEnable = true
            reset()
            // Only include arm64-v8a architecture
            //noinspection ChromeOsAbiSupport
            include("arm64-v8a")
            // Do not create a universal APK containing all ABIs
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.jetbrains.annotations)
    implementation(libs.android.material)
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Room Database (Day 9)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // Retrofit & OkHttp (Day 10)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime:2.9.0")

    implementation("", name = "androidsdk-release", ext = "aar")
    implementation("", name = "thermalsdk-release", ext = "aar")
}
