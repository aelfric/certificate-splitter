plugins {
    java
    application
    alias(libs.plugins.shadowJar)
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation(libs.aws.sdk)
    implementation(libs.pdfbox)
    implementation(libs.commons.csv)
    implementation(libs.slf4j.simple)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
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