plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val calismaNo = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.yasli.telefon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yasli.telefon"
        minSdk = 29
        targetSdk = 34
        versionCode = calismaNo
        versionName = "1.$calismaNo"
    }

    // Sabit imza: her yeni sürüm eskisinin üzerine sorunsuz kurulsun diye.
    signingConfigs {
        create("sabit") {
            storeFile = file("imza.jks")
            storePassword = "yasli123"
            keyAlias = "yasli"
            keyPassword = "yasli123"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sabit")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("sabit")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
