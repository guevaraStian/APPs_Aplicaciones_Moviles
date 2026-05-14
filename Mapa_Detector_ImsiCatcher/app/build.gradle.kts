plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {

    namespace = "com.example.towerscanner"

    compileSdk = 34

    defaultConfig {

        applicationId = "com.example.towerscanner"

        minSdk = 29

        targetSdk = 34

        versionCode = 1

        versionName = "1.0"
    }

    buildFeatures {

        viewBinding = true
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_17

        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {

        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.google.android.material:material:1.11.0")

    implementation("com.google.android.gms:play-services-maps:18.2.0")

    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("org.osmdroid:osmdroid-android:6.1.16")

    implementation("com.itextpdf:itext7-core:7.2.5")
}