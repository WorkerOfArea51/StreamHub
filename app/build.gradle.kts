import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// --- Secret loading: env vars first (CI / GitHub Actions), then local.properties (local dev) ---
// Mapping rule: streamhub.tmdb_api_key  →  STREAMHUB_TMDB_API_KEY  (env var)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

fun secret(key: String, default: String = ""): String {
    // 1. Environment variable (highest priority — CI / GitHub Actions)
    val envKey = key.uppercase().replace(".", "_")
    System.getenv(envKey)?.let { return it }

    // 2. local.properties (local development)
    localProps.getProperty(key)?.let { return it }

    // 3. Hardcoded default (never a real secret — always empty or "0000")
    return default
}

android {
    namespace = "com.streamhub.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.streamhub.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // --- Inject secrets into BuildConfig (never hardcode in source) ---
        buildConfigField("String", "TMDB_API_KEY", "\"${secret("streamhub.tmdb_api_key")}\"")
        buildConfigField("String", "MAL_CLIENT_ID", "\"${secret("streamhub.mal_client_id")}\"")
        buildConfigField("String", "MAL_CLIENT_SECRET", "\"${secret("streamhub.mal_client_secret")}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${secret("streamhub.telegram_bot_token")}\"")
        buildConfigField("String", "TELEGRAM_PRIVATE_CHANNEL_ID", "\"${secret("streamhub.telegram_channel_id")}\"")
        // Admin PIN stored as a bcrypt hash. Default "0000" disables admin login.
        buildConfigField("String", "ADMIN_PIN_HASH", "\"${secret("streamhub.admin_pin_hash", "0000")}\"")
        // Runtime flag — overwritten to "true" in debug build type below.
        buildConfigField("boolean", "DEBUG_LOGGING", "false")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ExoPlayer Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Image loading
    implementation(libs.coil.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Firebase
    implementation(libs.firebase.firestore.ktx)

    // Coroutines & Storage
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}
