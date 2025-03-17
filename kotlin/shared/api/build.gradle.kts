import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.openapi)
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
    runtimeOnly(libs.slf4j)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.junit)
    testImplementation(libs.junit.api)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("parseYaml", Exec::class) {
    commandLine(
        "sh",
        "-c",
        "yq .Resources.ZenobiaApi.Properties.DefinitionBody ../../../sam/lambda-stack.yml | " +
                "sed -E 's/!Sub //g' > " +
                "openapi.yml"
    )
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/openapi.yml")
    outputDir.set(layout.buildDirectory.dir("generated").get().toString())
    packageName.set("com.zenobiapay.api.generated")

    additionalProperties.set(
        mapOf(
            "serializationLibrary" to "jackson"
        )
    )
}

sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("generated/src/main/kotlin"))
}

tasks.named("openApiGenerate") {
    dependsOn("parseYaml")
}

tasks.named("build") {
    dependsOn("openApiGenerate")
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn("openApiGenerate")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
