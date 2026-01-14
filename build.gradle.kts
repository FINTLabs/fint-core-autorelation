plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "no.fintlabs"
version = project.findProperty("version") ?: "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.fintlabs.no/releases")
}

val fintVersion = "3.21.11-rc-4"
val fintTestVersion = "3.21.11-rc-4"
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("no.fint:fint-arkiv-resource-model-java:$fintVersion")
    implementation("no.fint:fint-okonomi-resource-model-java:$fintVersion")
    implementation("no.fint:fint-ressurs-resource-model-java:$fintVersion")
    implementation("no.fint:fint-utdanning-resource-model-java:$fintVersion")
    implementation("no.fint:fint-personvern-resource-model-java:$fintVersion")
    implementation("no.fint:fint-administrasjon-resource-model-java:$fintVersion")

    implementation("no.fintlabs:fint-kafka:3.2.0-rc-3")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.novari:fint-core-metamodel:4.0.0-rc-1")
    implementation("no.fintlabs:fint-core-autorelation-lib:6.0.0-rc-2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("no.fint:fint-arkiv-resource-model-java:${fintTestVersion}")
    testImplementation("no.fint:fint-okonomi-resource-model-java:${fintTestVersion}")
    testImplementation("no.fint:fint-ressurs-resource-model-java:${fintTestVersion}")
    testImplementation("no.fint:fint-utdanning-resource-model-java:${fintTestVersion}")
    testImplementation("no.fint:fint-personvern-resource-model-java:${fintTestVersion}")
    testImplementation("no.fint:fint-administrasjon-resource-model-java:${fintTestVersion}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}


sourceSets {
    val main by getting
    val resourceDir = "src/test/resources"

    val unit by creating {
        java.srcDir("src/test/unit/kotlin")
        resources.srcDir(resourceDir)
        compileClasspath += main.output + configurations.testCompileClasspath.get()
        runtimeClasspath += output + compileClasspath
    }

    val integration by creating {
        java.srcDir("src/test/integration/kotlin")
        resources.srcDir(resourceDir)
        compileClasspath += main.output + configurations.testCompileClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

configurations {
    val testImplementation by getting
    val testRuntimeOnly by getting

    val unitImplementation by getting { extendsFrom(testImplementation) }
    val unitRuntimeOnly by getting { extendsFrom(testRuntimeOnly) }

    val integrationImplementation by getting { extendsFrom(testImplementation) }
    val integrationRuntimeOnly by getting { extendsFrom(testRuntimeOnly) }
}

tasks {
    val unit = "unit"
    val integration = "integration"

    withType<Test> {
        useJUnitPlatform()
    }

    register<Test>(unit) {
        description = "Runs unit tests."
        group = "verification"
        testClassesDirs = sourceSets.named(unit).get().output.classesDirs
        classpath = sourceSets.named(unit).get().runtimeClasspath
    }

    register<Test>(integration) {
        description = "Runs integration tests."
        group = "verification"
        testClassesDirs = sourceSets.named(integration).get().output.classesDirs
        classpath = sourceSets.named(integration).get().runtimeClasspath
    }

    named("check") {
        dependsOn(unit, integration)
    }
}