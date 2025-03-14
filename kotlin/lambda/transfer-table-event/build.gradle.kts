import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
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
    api(project(":kotlin:shared"))
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api("com.amazonaws:aws-lambda-java-core:1.2.3")
    api("com.amazonaws:aws-lambda-java-events:3.11.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("com.squareup.okhttp3:okhttp:4.9.2")

    // Json processing
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.3")

    // Injection
    api("com.google.dagger:dagger:2.48")
    runtimeOnly("com.google.dagger:dagger-compiler:2.54")
    ksp("com.google.dagger:dagger-compiler:2.51.1")
    api("javax.inject:javax.inject:1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")

    // AWS
    implementation("software.amazon.awssdk:aws-core:2.30.22")
    implementation("software.amazon.awssdk:regions:2.30.22")
    implementation("software.amazon.awssdk:sdk-core:2.30.22")
    testImplementation("software.amazon.awssdk:utils:2.30.22")

    api("software.amazon.awssdk:dynamodb:2.29.45")
    api("software.amazon.awssdk:dynamodb-enhanced:2.29.47")
    api("software.amazon.awssdk:secretsmanager:2.29.45")
    api("software.amazon.awssdk:cognitoidentityprovider:2.29.31")
    api("software.amazon.awssdk:sqs:2.30.22")
    testImplementation("software.amazon.awssdk:utils:2.30.22")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("io.mockk:mockk-dsl:1.13.16")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // Plaid
    api("com.plaid:plaid-java:29.0.0")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.main {
    java.srcDir("${layout.buildDirectory}/generated/src/main/kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.zenobiapay.handlers.TransferHandler"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
