plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
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