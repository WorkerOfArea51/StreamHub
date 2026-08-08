import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// --- Secret loading: env vars first (CI / GitHub Actions), then local.properties (local dev) ---
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

fun secret(key: String, default: String = ""): String {
    val envKey = key.uppercase().replace(".", "_")
    System.getenv(envKey)?.let { return it }
    localProps.getProperty(key)?.let { return it }
    return default
}

android {
    namespace = "com.streamhub.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.streamhub.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 60
        versionName = "3.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TMDB_API_KEY", "\"${secret("streamhub.tmdb_api_key")}\"")
        buildConfigField("String", "MAL_CLIENT_ID", "\"${secret("streamhub.mal_client_id")}\"")
        buildConfigField("String", "MAL_CLIENT_SECRET", "\"${secret("streamhub.mal_client_secret")}\"")
        buildConfigField("String", "ADMIN_PIN_HASH", "\"${secret("streamhub.admin_pin_hash", "0000")}\"")
        buildConfigField("String", "TELEGRAM_API_ID", "\"${secret("streamhub.telegram_api_id")}\"")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${secret("streamhub.telegram_api_hash")}\"")
        buildConfigField("String", "TELEGRAM_ANIME_CHANNEL", "\"${secret("streamhub.telegram_anime_channel")}\"")
        buildConfigField("String", "TELEGRAM_MOVIES_CHANNEL", "\"${secret("streamhub.telegram_movies_channel")}\"")
        buildConfigField("String", "TELEGRAM_SERIES_CHANNEL", "\"${secret("streamhub.telegram_series_channel")}\"")
        buildConfigField("boolean", "DEBUG_LOGGING", "false")
        buildConfigField("String", "OWNER_USERNAMES", "\"${secret("streamhub.owner_usernames", "")}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    // FIX: Use public VariantOutput API for APK renaming — no internal AGP classes.
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            // Must be com.android.build.gradle.api.ApkVariantOutput (public API)
            // Cast is safe because all outputs of an application variant are APK outputs
            val output = this as com.android.build.gradle.api.ApkVariantOutput
            val abi = output.filters.find { it.filterType == "ABI" }?.identifier
            val archName = when (abi) {
                "armeabi-v7a" -> "arm32"
                "arm64-v8a" -> "arm64"
                else -> "universal"
            }
            output.outputFileName = "StreamHub-${archName}-${variant.buildType.name}.apk"
        }
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) {
            load(FileInputStream(keystorePropsFile))
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            } else {
                // No keystore.properties — fall back to debug signing so builds don't fail.
                // Release APKs will NOT be properly signed for Play Store distribution.
                logger.warn("WARNING: keystore.properties missing. Falling back to debug signing. " +
                    "Release APKs will NOT be properly signed for Play Store.")
                storeFile = signingConfigs.getByName("debug").storeFile
                storePassword = signingConfigs.getByName("debug").storePassword
                keyAlias = signingConfigs.getByName("debug").keyAlias
                keyPassword = signingConfigs.getByName("debug").keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
            // FIX #2: Debug uses debug signing — works out of the box, no keystore.properties needed
            signingConfig = signingConfigs.getByName("debug")
            // FIX #3: Distinguish debug APKs by version name suffix
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
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
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)

    implementation(libs.coil.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.firebase.firestore.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.bcrypt)
    implementation("com.google.zxing:core:3.5.3")

    // TDLib — Telegram Database Library (native MTProto client)
    implementation(libs.tdlib.java)

    // FIX: Test dependencies for unit and instrumented tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    debugImplementation(libs.androidx.ui.tooling)
}
