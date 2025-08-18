/*
 * Boilerplate plugin module for Valtimo plugins
 */

dockerCompose {
    setProjectName("__ARTIFACT_NAME__")
    isRequiredBy(project.tasks.integrationTesting)
    tasks.integrationTesting {
        useComposeFiles.addAll("$rootDir/docker-resources/docker-compose-base-test.yml")
    }
}


dependencies {
    implementation("com.ritense.valtimo:contract")
    implementation("com.ritense.valtimo:core")
    implementation("com.ritense.valtimo:plugin-valtimo")

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

apply(from = "gradle/publishing.gradle")
