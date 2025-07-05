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
    implementation(project(":kotlin:shared:api"))
    implementation(project(":kotlin:shared:api:model"))
    implementation(project(":kotlin:shared:orum"))
    implementation(project(":kotlin:shared:events"))
    implementation(project(":kotlin:shared:plaid"))
    implementation(project(":kotlin:shared:metrics"))
    implementation(project(":kotlin:shared:cryptography"))
    implementation(project(":kotlin:shared:table"))
    implementation(project(":kotlin:shared:table:bank"))
    implementation(project(":kotlin:shared:table:transfer"))
    implementation(project(":kotlin:shared:table:user"))

    api(libs.kotlin.stdlib)
    api(libs.kotlin.coroutines)
    api(libs.lambda.core)
    api(libs.lambda.events)

    // Json processing
    runtimeOnly(libs.jackson.databind)
    runtimeOnly(libs.jackson.kotlin)

    // Injection
    api(libs.dagger)
    ksp(libs.dagger.compiler)
    api(libs.jakarta.inject)

    // JSON
    runtimeOnly(libs.jackson.core)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j)

    // AWS
    implementation(libs.aws.dynamodb)
    implementation(libs.aws.dynamodb.enhanced)
    implementation(libs.aws.secretsmanager)
    implementation(libs.aws.cloudwatch)
    api(libs.aws.sqs)

    // Plaid
    implementation(libs.plaid)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit)
    testImplementation(libs.jackson.kotlin)
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
            // Exclude these classes from minimization as they might be loaded via reflection
            exclude(dependency("com.google.dagger:dagger:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-common:.*"))
            
            // Keep only the specific Bouncy Castle classes we need
            // This ensures only the classes actually used by our code are included
            include(dependency("org.bouncycastle:.*:.*"))
        }
        
        // Merge service files to avoid duplication
        mergeServiceFiles()
        
        // Exclude unnecessary files
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
        exclude("META-INF/versions/**")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/*.MF")
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/DEPENDENCIES")
        exclude("about.html")
        exclude("about_files/**")
        exclude("plugin.properties")
        exclude("plugin.xml")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_module")
        exclude("**/*.kotlin_builtins")
        
        // Exclude development tools
        exclude("org/openjdk/tools/**")
        exclude("com/google/googlejavaformat/**")
        
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
