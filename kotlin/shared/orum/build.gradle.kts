import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
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

dependencies {
    api(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    api(libs.dagger)
    runtimeOnly(libs.dagger.compiler)
    ksp(libs.dagger.compiler)

    // deserialization
    api(libs.javax.inject)
    runtimeOnly(libs.jackson.core)
    testImplementation(libs.jackson.kotlin)
    api(libs.jackson.databind)
    api(libs.jackson.annotations)

    // Http
    implementation(libs.okhttp)
    api(platform(libs.okhttp.bom))

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.slf4j)

    // AWS
    api(libs.aws.secretsmanager)

    // Testing
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.junit)
    testImplementation(libs.junit.api)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
