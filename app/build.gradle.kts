
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.spotifly"
    compileSdk = 34

    // New Workaround to edit the "manifestPlaceholders"
    val myManifestPlaceholders = mapOf("redirectSchemeName" to "spotifly", "redirectHostName" to "callback")

    defaultConfig {
        applicationId = "com.example.spotifly"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Fix?
        manifestPlaceholders.putAll(myManifestPlaceholders)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Splash Screen Api
    implementation("androidx.core:core-splashscreen:1.0.0")

    // Spotify Api
    //implementation("com.spotify.android.appremote:app-remote:0.7.2")
    implementation("com.spotify.android:auth:2.0.1")

    // HTTP
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    //implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")


}