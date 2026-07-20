plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.smz70.mmhue.watch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.smz70.mmhue.watch"
        // Wear OS 4 and up. The Pixel Watch 4 ships Wear OS 6 (API 36).
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        // The panel lives on the home LAN over plain HTTP; see network_security_config.
        buildConfigField("String", "MMHUE_BASE_URL", "\"http://mm.fritz.box\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Minified because the watch installs over ADB Wi-Fi and the
            // unminified build is slow enough to time out mid-transfer.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Debug-signed on purpose: this is sideloaded, never published.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
