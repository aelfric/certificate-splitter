plugins {
    java
    jacoco
    application
    alias(libs.plugins.shadowJar)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.aws.sdk)
    implementation(libs.pdfbox)
    implementation(libs.commons.csv)
    implementation(libs.slf4j.simple)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.junit)
    testImplementation(libs.assertJ)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    mainClass.set("org.nycfl.certificates.loader.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}