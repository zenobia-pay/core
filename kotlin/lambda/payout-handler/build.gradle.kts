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
    api(project(":kotlin:shared"))
    implementation(project(":kotlin:shared:orum"))
    implementation(project(":kotlin:shared:table"))
    implementation(project(":kotlin:shared:table:transfer"))
    implementation(project(":kotlin:shared:table:user"))
    implementation(project(":kotlin:shared:events"))
    api(project(":kotlin:shared:metrics"))

    api(libs.kotlin.stdlib)
    api(libs.lambda.core)
    api(libs.lambda.events)

    // Json processing
    api(libs.jackson.databind)
    implementation(libs.jackson.kotlin)

    // Injection
    api(libs.dagger)
    ksp(libs.dagger.compiler)
    api(libs.jakarta.inject)

    // JSON
    implementation(libs.jackson.core)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j)

    // AWS
    api(libs.aws.dynamodb)
    api(libs.aws.dynamodb.enhanced)
    api(libs.aws.sqs)
    api(libs.aws.cloudwatch)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit)
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

tasks {
    shadowJar {
        archiveBaseName.set("lambda")
        archiveClassifier.set("")
        archiveVersion.set("")
        manifest {
            attributes(mapOf("Main-Class" to "com.zenobiapay.user.handlers.UserHandler"))
        }
    }
    jar {
        enabled = false
    }
    build {
        dependsOn(shadowJar)
    }
}
