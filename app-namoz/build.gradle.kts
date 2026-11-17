import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun local(key: String): String? = localProps.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "uz.xumoyun.namoz"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.xumoyun.namoz"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            local("keystore.file")?.let { storeFile = file(it) }
            storePassword = local("keystore.password")
            keyAlias = local("keystore.alias")
            keyPassword = local("keystore.aliasPassword")
        }
    }

    buildTypes {
        debug {
            // Ishlab chiqishda faqat test reklama ID lari.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_REWARDED", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (local("keystore.file") != null) signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["admobAppId"] = local("admob.appId") ?: "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_BANNER", "\"${local("admob.banner") ?: "ca-app-pub-3940256099942544/9214589741"}\"")
            buildConfigField("String", "AD_REWARDED", "\"${local("admob.rewarded") ?: "ca-app-pub-3940256099942544/5224354917"}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":core-data"))
    implementation(project(":core-ads"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.adhan)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
