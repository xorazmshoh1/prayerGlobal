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
    compileSdk = 36

    defaultConfig {
        applicationId = "uz.xumoyun.namozhifz"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (local("keystore.file") != null) signingConfig = signingConfigs.getByName("release")
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
    implementation(project(":feature-prayer"))
    implementation(project(":feature-hifz"))
    implementation(project(":feature-qibla"))
    implementation(project(":feature-tasbih"))
    implementation(project(":feature-calendar"))
    implementation(project(":feature-ramadan"))

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

// TZ 10-bo'lim: reklama, billing va analytics SDK'lari bog'liqliklar daraxtida bo'lsa build to'xtaydi.
val forbiddenDependencies = listOf(
    "com.google.android.gms:play-services-ads",
    "com.google.android.gms:play-services-measurement",
    "com.google.android.ump:",
    "com.google.firebase:firebase-ads",
    "com.google.firebase:firebase-analytics",
    "com.android.billingclient:",
    "com.facebook.android:audience-network-sdk",
    "com.applovin:",
    "com.unity3d.ads:",
    "com.ironsource.",
    "com.vungle:",
    "com.inmobi.",
    "com.chartboost:",
    "com.startapp:",
    "com.mbridge.",
    "com.yandex.android:mobileads",
    "com.pangle.",
    "com.bytedance.sdk:pangle",
)

val verifyNoAds by tasks.registering {
    group = "verification"
    description = "Reklama/billing/analytics SDK'lari bog'liqliklarda yo'qligini tekshiradi."
    val classpaths = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath").map { configurations.named(it) }
    doLast {
        val found = classpaths.flatMap { cp ->
            cp.get().incoming.resolutionResult.allComponents.mapNotNull { c ->
                val id = c.moduleVersion ?: return@mapNotNull null
                val coordinate = "${id.group}:${id.name}"
                coordinate.takeIf { coord -> forbiddenDependencies.any { coord.startsWith(it) } }
            }
        }.distinct()
        if (found.isNotEmpty()) {
            throw GradleException("Reklama/billing/analytics SDK topildi (TZ 10-bo'lim): ${found.joinToString()}")
        }
        logger.lifecycle("verifyNoAds: reklama SDK'lari topilmadi")
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyNoAds) }
