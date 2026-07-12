plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "breui"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation("com.googlecode.lanterna:lanterna:3.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("breui")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "breui.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}
