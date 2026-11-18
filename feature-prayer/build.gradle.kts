plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "uz.xumoyun.feature.prayer"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // docs/prayer-tables ni qayta yaratish: ./gradlew :feature-prayer:testDebugUnitTest -Dprayer.tables.write=true
        unitTests.all { it.systemProperty("prayer.tables.write", System.getProperty("prayer.tables.write") ?: "false") }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":core-data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    // Entity va DAO shu modulda; @Database va Room kompilyatori ilova modulida.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // adhan turlari tashqariga chiqmaydi: ommaviy API — CalcInput, DayTimes, PrayerCalculator.
    implementation(libs.adhan)
    // Bildirishnomalar va vidjet (3-bosqich).
    implementation(libs.work.runtime)
    implementation(libs.glance.appwidget)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
