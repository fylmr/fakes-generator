plugins {
    kotlin("jvm")
}

// Versions are declared in 'gradle.properties' file
val kspVersion: String by project
val kotlinPoetVersion: String by project

dependencies {
    implementation(project(":annotations"))

    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}