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
    implementation(project(":kotlin:shared:metrics"))
    implementation(project(":kotlin:shared:api:model"))
    implementation(project(":kotlin:shared:table"))
    implementation(project(":kotlin:shared:events"))
    implementation(project(":kotlin:shared:table:rds"))

    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    api(libs.lambda.core)
    api(libs.lambda.events)

    // Json processing
    runtimeOnly(libs.jackson.databind)
    runtimeOnly(libs.jackson.kotlin)
    runtimeOnly(libs.jackson.core)
    runtimeOnly(libs.postgresql)

    // Injection
    api(libs.dagger)
    ksp(libs.dagger.compiler)
    api(libs.jakarta.inject)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j)

    // AWS
    implementation(libs.aws.secretsmanager)
    implementation(libs.aws.cloudwatch)
    api(libs.aws.lambda)
    implementation(libs.aws.s3)
    implementation(libs.aws.http.client)
    implementation(libs.okhttp)

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
        
        // Enable minimization to remove unused classes
        minimize {
            // Exclude AWS HTTP client classes from minimization
            exclude(dependency("software.amazon.awssdk:apache-client:.*"))
            // Exclude Kotlin reflection classes from minimization
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            // Exclude Jackson Kotlin module classes from minimization
            exclude(dependency("com.fasterxml.jackson.module:jackson-module-kotlin:.*"))
            // Exclude Jackson Nullable module classes from minimization
            exclude(dependency("org.openapitools:jackson-databind-nullable:.*"))
            // Exclude DynamoDB enhanced client classes from minimization
            exclude(dependency("software.amazon.awssdk:dynamodb-enhanced:.*"))
            // Exclude PostgreSQL JDBC driver from minimization
            exclude(dependency("org.postgresql:postgresql:.*"))
            // Exclude Log4j and SLF4J classes from minimization
            exclude(dependency("org.apache.logging.log4j:log4j-core:.*"))
            exclude(dependency("org.apache.logging.log4j:log4j-slf4j2-impl:.*"))
            exclude(dependency("org.slf4j:slf4j-api:.*"))
            // Exclude AWS X-Ray classes from minimization
            exclude(dependency("com.amazonaws:aws-xray-recorder-sdk-core:.*"))
            exclude(dependency("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2:.*"))
            exclude(dependency("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2-instrumentor:.*"))
            exclude(dependency("com.amazonaws:aws-java-sdk-xray:.*"))
        }
        
        // Merge service files to avoid duplication
        mergeServiceFiles()
        
        // Preserve service provider configuration files
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer::class.java)
        
        // Exclude unnecessary files
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("mozilla/public-suffix-list.txt")
        
        // Exclude development tools that shouldn't be in runtime
        exclude("org/openjdk/tools/**")
        exclude("com/google/googlejavaformat/**")
        
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
    
    // Add JAR analysis task
    register("analyzeJar") {
        dependsOn("shadowJar")
        doLast {
            val jarFile = shadowJar.get().archiveFile.get().asFile
            println("JAR size: ${jarFile.length() / (1024 * 1024)} MB")
            
            // Print largest files in JAR
            exec {
                commandLine("sh", "-c", "unzip -l ${jarFile.absolutePath} | sort -k1,1nr | head -20")
            }
        }
    }
}
