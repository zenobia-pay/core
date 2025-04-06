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
    implementation(project(":kotlin:shared:api:model"))
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.bouncycastle.bcprov)
    api(libs.kotlin.stdlib)
    api(libs.dagger)
    runtimeOnly(libs.dagger.compiler)
    ksp(libs.dagger.compiler)

    // deserialization
    api(libs.javax.inject)
    testImplementation(libs.jackson.kotlin)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.slf4j)

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
