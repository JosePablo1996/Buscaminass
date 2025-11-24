plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.buscaminas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.buscaminas"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Para GitHub Actions
            storeFile = file("${project.rootDir}/buscaminas.jks")
            storePassword = System.getenv("RELEASE_PASSWORD") ?: ""
            keyAlias = System.getenv("RELEASE_ALIAS") ?: ""
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
        }

        create("debugSigned") {
            // Para desarrollo local - FORZAR con extensión .jks
            storeFile = file("${project.rootDir}/buscaminas_key.jks")
            storePassword = "DaymonMiranda@#2025"
            keyAlias = "key_pablo"
            keyPassword = "DaymonMiranda@#2025"
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debugSigned")
        }

        getByName("release") {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Forzar nuestro signing config y evitar externalOverride
            signingConfig = signingConfigs.getByName("debugSigned")
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
        viewBinding = true
    }

    // Agregar esto para evitar que Android Studio inyecte configuración externa
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Agregar esta tarea para verificar que el keystore existe
tasks.register("verifyKeystore") {
    doFirst {
        val keystoreFile = file("${project.rootDir}/buscaminas_key.jks")
        if (keystoreFile.exists()) {
            println("✅ Keystore encontrado: ${keystoreFile.absolutePath}")
        } else {
            println("❌ Keystore NO encontrado: ${keystoreFile.absolutePath}")
            throw GradleException("Keystore no encontrado: ${keystoreFile.absolutePath}")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("verifyKeystore")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}