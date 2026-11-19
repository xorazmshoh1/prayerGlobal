// TZ 5-bosqich: Tanzil matnidan quran.db yaratish vositasi (Android'siz, alohida JVM loyiha).
plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    testImplementation("junit:junit:4.13.2")
}


application {
    mainClass.set("uz.xumoyun.tools.quran.MainKt")
}

tasks.test {
    // Haqiqiy Tanzil fayllari: -Ptanzil.dir=/yo'l yoki TANZIL_DIR env. Fotiha xeshi: -Pquran.fatihaSha256=...
    listOf("tanzil.dir", "quran.fatihaSha256").forEach { key ->
        val value = (project.findProperty(key) as String?) ?: System.getenv(key.uppercase().replace('.', '_'))
        if (value != null) systemProperty(key, value)
    }
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}
