plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

application {
    mainClass.set("com.phhmaa.MainKt")
}

kotlin.sourceSets.main {
    kotlin.srcDirs(
        file("$buildDir/generated/ksp/main/kotlin"),
    )
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))
}
