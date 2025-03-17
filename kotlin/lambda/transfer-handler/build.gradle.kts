import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
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
    implementation(project(":kotlin:shared:api"))
    implementation(project(":kotlin:shared:orum"))
    implementation(project(":kotlin:shared:cognito"))
    implementation(project(":kotlin:shared:table"))
    implementation(project(":kotlin:shared:table:bank"))
    implementation(project(":kotlin:shared:table:transfer"))
    implementation(project(":kotlin:shared:table:user"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Json processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")

    // Injection
    implementation("com.google.dagger:dagger:2.48")
    implementation("com.google.dagger:dagger-compiler:2.54")
    implementation("com.google.dagger:dagger-compiler:2.51.1")
    ksp("com.google.dagger:dagger-compiler:2.51.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")

    // Testing
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // AWS
    api("software.amazon.awssdk:secretsmanager:2.30.22")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        archiveBaseName.set("lambda")
        archiveClassifier.set("")
        archiveVersion.set("")
        manifest {
            attributes(mapOf("Main-Class" to "com.zenobiapay.transfer.handlers.TransferHandler"))
        }
    }
    jar {
        enabled = false
    }
    build {
        dependsOn(shadowJar)
    }
}
