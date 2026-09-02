import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// --- Secret loading: local.properties, project properties, and all CI env var variations ---
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        try {
            file.inputStream().use { load(it) }
        } catch (_: Exception) {}
    }
}

fun secret(key: String, default: String = ""): String {
    val localVal = localProps.getProperty(key)?.trim()
    if (!localVal.isNullOrBlank()) return localVal

    if (project.hasProperty(key)) {
        val prop = project.property(key)?.toString()?.trim()
        if (!prop.isNullOrBlank()) return prop
    }

    val envKey = key.uppercase().replace(".", "_")
    val envShort = envKey.removePrefix("STREAMHUB_")
    System.getenv(envKey)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    System.getenv(envShort)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    System.getenv(key)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

    return default
}

android {
    namespace = "com.streamhub.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streamhub.app"
        minSdk = 24
        targetSdk = 35
        val envVersionName = System.getenv("VERSION_NAME")?.removePrefix("v")?.removePrefix("V")
        versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 248
        versionName = if (!envVersionName.isNullOrBlank()) envVersionName else "4.8.248"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TMDB_API_KEY", "\"${secret("streamhub.tmdb_api_key")}\"")
        buildConfigField("String", "MAL_CLIENT_ID", "\"${secret("streamhub.mal_client_id")}\"")
        buildConfigField("String", "MAL_CLIENT_SECRET", "\"${secret("streamhub.mal_client_secret")}\"")
        buildConfigField("boolean", "DEBUG_LOGGING", "false")
        buildConfigField("String", "ADMIN_MASTER_PASSWORD", "\"${secret("streamhub.admin_master_password", "")}\"")
        buildConfigField("String", "APP_ACCESS_CODE", "\"${secret("streamhub.app_access_code", "")}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
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
                "x86_64" -> "x86_64"
                null -> "universal"
                else -> abi
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
                // Fallback to debug keystore for CI/local testing when keystore.properties is not present
                val debugSigning = signingConfigs.getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                keyAlias = debugSigning.keyAlias
                keyPassword = debugSigning.keyPassword
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.3.1+1")
    implementation("com.github.TeamNewPipe.NewPipeExtractor:extractor:v0.24.4")

    implementation(libs.coil.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.firebase.firestore.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    // NOTE: Using 1.1.0-alpha06 for KeyScheme.AES256_GCM support (not available in 1.0.0 stable)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // FIX: Test dependencies for unit and instrumented tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    debugImplementation(libs.androidx.ui.tooling)
}
