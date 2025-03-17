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
    implementation(project(":kotlin:shared"))
    api(libs.kotlin.stdlib)

    // Json processing
    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    api(libs.jackson.databind)
    api(libs.jackson.annotations)

    // Injection
    api(libs.dagger)
    ksp(libs.dagger.compiler)
    api(libs.javax.inject)

    // HTTP
    api(libs.okhttp)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j)

    // AWS
    implementation(libs.aws.core)
    implementation(libs.aws.regions)
    implementation(libs.aws.sdk.core)
    testImplementation(libs.aws.sdk.utils)

    api(libs.aws.dynamodb)
    api(libs.aws.dynamodb.enhanced)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.junit)
    testImplementation(libs.junit.api)

    // Plaid
    api(libs.plaid)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

