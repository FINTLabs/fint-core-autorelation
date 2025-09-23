import org.gradle.kotlin.dsl.implementation

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

val fintVersion = "3.19.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("no.fint:fint-utdanning-resource-model-java:$fintVersion")
    implementation("no.fint:fint-administrasjon-resource-model-java:$fintVersion")
    implementation("no.fint:fint-personvern-resource-model-java:$fintVersion")
    implementation("no.fint:fint-okonomi-resource-model-java:$fintVersion")
    implementation("no.fint:fint-ressurs-resource-model-java:$fintVersion")
    implementation("no.fint:fint-arkiv-resource-model-java:$fintVersion")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.fintlabs:fint-kafka:3.0.0-rc-1")
    implementation("no.fintlabs:fint-core-consumer-metamodel:2.0.0-rc-4")
    implementation("no.fintlabs:fint-core-autorelation-lib:2.0.0-rc-2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
