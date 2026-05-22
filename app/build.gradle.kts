plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.jluqgon214.alphabot2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jluqgon214.alphabot2"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "**/module-info.class"

            excludes += "org/bouncycastle/x509/CertPathReviewerMessages_de.properties"        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Dependencias de Firebase
    implementation("com.google.firebase:firebase-auth") // Para Firebase Authentication
    implementation("com.google.firebase:firebase-firestore") // Para Cloud Firestore
    implementation("com.google.firebase:firebase-storage") // Para Cloud Storage para Firebase

    // JSch library for SSH connections
    implementation("com.jcraft:jsch:0.1.55")

    // Kotlin Coroutines for asynchronous programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Joystick library
    implementation("com.github.manalkaff:JetStick:1.2")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Canopas Animated Bottom Navigation Bar
    implementation("com.canopas.compose-animated-navigationbar:bottombar:1.0.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

     // UCrop for interactive image cropping
     // UCrop:
     // - Librería para recorte interactivo de imágenes.
     // - La usamos para que el usuario elija el recorte de su foto de perfil.
     implementation("com.github.yalantis:ucrop:2.2.8")

     // AppCompat (required by UCropActivity)
     // AppCompat:
     // - UCropActivity está basada en AppCompatActivity y necesita appcompat.
     // - También usamos un tema AppCompat en el AndroidManifest para la pantalla de UCrop.
      implementation("androidx.appcompat:appcompat:1.7.0")

      // DataStore Preferences (guardar opciones como "recordarme")
      implementation("androidx.datastore:datastore-preferences:1.0.0")
}