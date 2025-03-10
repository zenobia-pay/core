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

dependencies {
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

    // AWS
    implementation("software.amazon.awssdk:dynamodb:2.29.45")
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.29.47")
    implementation("software.amazon.awssdk:secretsmanager:2.29.45")
    implementation("software.amazon.awssdk:cognitoidentityprovider:2.29.31")
    implementation("software.amazon.awssdk:sqs:2.30.22")

    // Testing
    testImplementation("io.mockk:mockk:1.13.16")

    // Plaid
    implementation("com.plaid:plaid-java:29.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("parseYaml", Exec::class) {
    commandLine(
        "sh",
        "-c",
        "yq .Resources.ZenobiaApi.Properties.DefinitionBody sam/lambda-stack.yml | " +
                "sed -E 's/!Sub //g' > " +
                "openapi.yml"
    )
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/openapi.yml")
    outputDir.set("$buildDir/generated")
    packageName.set("com.zenobiapay.generated")

    additionalProperties.set(
        mapOf(
            "serializationLibrary" to "jackson"
        )
    )
}

sourceSets.main {
    java.srcDir("$buildDir/generated/src/main/kotlin")
}

tasks.named("openApiGenerate") {
    dependsOn("parseYaml")
}

tasks.named("build") {
    dependsOn("openApiGenerate")
}

tasks.matching { it.name.startsWith("kapt") }.configureEach {
    dependsOn("openApiGenerate")
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn("openApiGenerate")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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