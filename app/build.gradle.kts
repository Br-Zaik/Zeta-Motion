plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeta.fondo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zeta.fondo"
        minSdk = 26
        targetSdk = 36
        versionCode = 37
        versionName = "36.0.0"
        manifestPlaceholders["zetaAppLabel"] = "@string/app_name"
    }

    // Una variante BETA, separada de instalaciones anteriores, SIEMPRE firmada.
    // Esta clave es PUBLICA, exclusiva de pruebas, y NUNCA se usará para Google Play.
    // Conserva el mismo certificado entre compilaciones para poder actualizar la beta.
    // NO confundir esta clave de pruebas con la clave privada de producción.
    val prodStore = System.getenv("ZETA_KEYSTORE_PATH")
    val prodPass = System.getenv("ZETA_KEYSTORE_PASSWORD")
    val prodAlias = System.getenv("ZETA_KEY_ALIAS")
    val prodKeyPass = System.getenv("ZETA_KEY_PASSWORD")
    val prodSigningConfigured = listOf(prodStore, prodPass, prodAlias, prodKeyPass)
        .all { !it.isNullOrBlank() }

    signingConfigs {
        create("betaPublicTestOnly") {
            storeFile = file("zeta-beta-public-only.jks")
            storePassword = "publicBetaOnly-2026"
            keyAlias = "zeta_beta"
            keyPassword = "publicBetaOnly-2026"
        }
        if (prodSigningConfigured) {
            create("zetaPrivateRelease") {
                storeFile = file(prodStore!!)
                storePassword = prodPass!!
                keyAlias = prodAlias!!
                keyPassword = prodKeyPass!!
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".beta"
            manifestPlaceholders["zetaAppLabel"] = "Zeta Motion Beta"
            signingConfig = signingConfigs.getByName("betaPublicTestOnly")
        }
        release {
            // Incluso con un workflow antiguo que ejecute assembleRelease,
            // NUNCA entregar un app-release-unsigned.apk.
            if (prodSigningConfigured) {
                signingConfig = signingConfigs.getByName("zetaPrivateRelease")
            } else {
                applicationIdSuffix = ".beta"
                manifestPlaceholders["zetaAppLabel"] = "Zeta Motion Beta"
                signingConfig = signingConfigs.getByName("betaPublicTestOnly")
            }
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
