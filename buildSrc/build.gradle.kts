plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
}

gradlePlugin {
    plugins {
        create("scaffold") {
            id = "com.ritense.scaffold"
            implementationClass = "scaffold.ScaffoldPlugin"
            displayName = "Scaffold Plugin"
            description = "Scaffold backend/frontend projects from templates with placeholders"
        }
    }
}