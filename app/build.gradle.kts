import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun localProperty(key: String): String? =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

/** Google's official sample AdMob ids — safe for debug builds only. */
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerId = "ca-app-pub-3940256099942544/6300978111"

android {
    namespace = "fi.attenka.VisualMessage"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "fi.attenka.VisualMessage"
        minSdk = 24
        targetSdk = 36
        versionCode = 18
        versionName = "1.0.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = admobTestAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperty("release.store.file") ?: return@create
            storeFile = rootProject.file(storeFilePath)
            storePassword = localProperty("release.store.password")
            keyAlias = localProperty("release.key.alias")
            keyPassword = localProperty("release.key.password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val prodAppId = localProperty("admob.app.id") ?: admobTestAppId
            val prodBannerId = localProperty("admob.banner.id") ?: admobTestBannerId
            manifestPlaceholders["admobAppId"] = prodAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$prodBannerId\"")

            val releaseKeystore = signingConfigs.getByName("release").storeFile
            if (releaseKeystore?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.graphics.path)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
