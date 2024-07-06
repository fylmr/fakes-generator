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
}