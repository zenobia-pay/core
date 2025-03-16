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
    implementation(project(":kotlin:shared:api"))
    api(libs.kotlin.stdlib)
    api(libs.lambda.core)
    api(libs.lambda.events)

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
    api(libs.aws.secretsmanager)
    api(libs.aws.cognito)
    api(libs.aws.sqs)

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

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.zenobiapay.handlers.BankHandler"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
