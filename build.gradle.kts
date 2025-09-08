import org.gradle.kotlin.dsl.implementation

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "no.fintlabs"
version = "0.0.1-SNAPSHOT"
description = "fint-core-autorelation"

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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.fintlabs:fint-kafka:3.0.0-rc-1")
    implementation("no.fintlabs:fint-core-consumer-metamodel:2.0.0-rc-3")
    implementation("no.fintlabs:fint-core-autorelation-model:1.3.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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
