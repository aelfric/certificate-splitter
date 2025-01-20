plugins {
    id("java")
    id("maven-publish")
    id("application")
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("software.amazon.awssdk:s3:2.17.96")
    implementation("org.apache.pdfbox:pdfbox:2.0.27")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("info.picocli:picocli:4.7.0")
    annotationProcessor("info.picocli:picocli-codegen:4.7.0")
}

group = "com.frankriccobono"
version = "0.0.3-SNAPSHOT"
description = "certs-splitter"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:-processing")
    options.compilerArgs.add("-Werror")
    options.encoding = "UTF-8"
}

application {
    mainClassName = "org.nycfl.certificates.loader.Main"
}