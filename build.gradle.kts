import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("org.openapi.generator") version "7.12.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "com.zenobiapay"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
