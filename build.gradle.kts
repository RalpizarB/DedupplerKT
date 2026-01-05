plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
}

group = "com.deduppler"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    // Blake2b hashing
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    // Apache Commons CSV
    implementation("org.apache.commons:commons-csv:1.10.0")
}

compose.desktop {
    application {
        mainClass = "com.deduppler.MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "DedupplerKT"
            packageVersion = "1.0.0"
            description = "Duplicate File Finder"
            
            windows {
                menuGroup = "DedupplerKT"
                upgradeUuid = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
