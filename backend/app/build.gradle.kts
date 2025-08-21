dependencies {
    implementation(platform("com.ritense.valtimo:valtimo-dependency-versions"))

    implementation("com.ritense.valtimo:valtimo-dependencies")
    implementation("com.ritense.valtimo:valtimo-gzac-dependencies")
    implementation("com.ritense.valtimo:local-mail")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.postgresql:postgresql")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    if (System.getProperty("os.arch") == "aarch64") {
        runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.105.Final:osx-aarch_64")
    }
    // Plugins

    implementation(project(":backend:made-with-ez2"))
}

tasks.jar {
    enabled = false
}

apply(from = "../../gradle/environment.gradle.kts")
val configureEnvironment = extra["configureEnvironment"] as (task: ProcessForkOptions) -> Unit

tasks.bootRun {
    dependsOn("composeUp")
    systemProperty("spring.profiles.include", "dev")
    val t = this
    doFirst {
        configureEnvironment(t)
    }
}
