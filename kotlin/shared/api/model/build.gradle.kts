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

    // Json processing
    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    api(libs.jackson.databind)
    api(libs.jackson.annotations)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.nullable)

    // Injection
    api(libs.dagger)
    ksp(libs.dagger.compiler)
//    api(libs.jakarta.inject)
    api(libs.jakarta.validation)
    api(libs.jakarta.annotation)
}

tasks.test {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi.yml")
    outputDir.set(layout.buildDirectory.dir("generated").get().toString())
    packageName.set("com.zenobiapay.api.generated")
    modelPackage.set("com.zenobiapay.api.generated.model")

    additionalProperties.set(
        mapOf(
            "library" to "native",
            "useBeanValidation" to "true",
            "useJakartaEe" to "true"
        )
    )
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated/src/main/java"))
}

tasks.jar {
    from(layout.buildDirectory.dir("generated/src/main/java"))
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
