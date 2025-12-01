import shadow.bundletool.com.android.tools.r8.internal.li


plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs")
}

android {
    namespace = "com.example.lottos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lottos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // APP - No changes needed here, these are fine.
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // FIREBASE - No changes needed here.
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.firebase.installations)

    // CORE LIBS - No changes needed.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // --- UNIT TESTS (src/test) ---
    // These run on your local computer (JVM).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.robolectric:robolectric:4.16")
    // For testing Navigation components locally.
    testImplementation("androidx.navigation:navigation-testing:2.7.7")

    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")


    // --- INSTRUMENTATION TESTS (src/androidTest) ---
    // These run on an Android device or emulator.
    // Core AndroidX Test libraries.
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Espresso for UI interactions.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")

    // FragmentScenario for testing Fragments in isolation.
    androidTestImplementation("androidx.fragment:fragment-testing:1.8.9")

    // For testing Navigation components in instrumented tests.
    androidTestImplementation("androidx.navigation:navigation-testing:2.9.6")
}




