import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hashpony.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hashpony.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // No default vector-drawable/appcompat shims needed; zero-permission app.
    }

    signingConfigs {
        // Release signing is driven by keystore.properties at the repo root
        // (gitignored), so no keystore path or password is ever committed. Keys:
        // storeFile, storePassword, keyAlias, keyPassword. When the file is
        // absent (CI, a clean checkout) the release build is left unsigned
        // rather than failing.
        create("release") {
            val keystoreProps = rootProject.file("keystore.properties")
            if (keystoreProps.exists()) {
                val props = Properties()
                keystoreProps.inputStream().use { props.load(it) }
                val storePath = props.getProperty("storeFile")
                if (storePath != null) {
                    storeFile = file(storePath)
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    // QR encoder only (pure Java, no camera, no network). Scanning is handed to
    // an installed scanner app, so HashPony stays zero-permission.
    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
