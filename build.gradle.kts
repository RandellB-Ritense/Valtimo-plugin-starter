import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.os.Os.FAMILY_MAC
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

val valtimoVersion: String by project

plugins {
    // Idea
    idea
    id("org.jetbrains.gradle.plugin.idea-ext")

    // Spring
    id("org.springframework.boot")
    id("io.spring.dependency-management")

    // Kotlin
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")

    // Other
    id("com.avast.gradle.docker-compose")
    id("cn.lalaki.central") version "1.2.5"

    id("com.ritense.scaffold")

}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://repo.ritense.com/repository/maven-public/") }
        maven { url = uri("https://repo.ritense.com/repository/maven-snapshot/") }
    }
}

subprojects {
    println("Configuring ${project.path}")

    if (project.path.startsWith(":backend")) {

        tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
            mainClass.set("com.ritense.valtimoplugins.sandbox.PluginApplication")
        }
        apply(plugin = "java")
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")

        apply(plugin = "idea")
        apply(plugin = "java-library")
        apply(plugin = "kotlin")
        apply(plugin = "kotlin-spring")
        apply(plugin = "kotlin-jpa")
        apply(plugin = "com.avast.gradle.docker-compose")
        apply(plugin = "maven-publish")

        java.sourceCompatibility = JavaVersion.VERSION_17
        java.targetCompatibility = JavaVersion.VERSION_17

        tasks.withType<KotlinCompile> {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_17
                javaParameters = true
            }
        }

        dependencies {
            implementation(platform("com.ritense.valtimo:valtimo-dependency-versions:$valtimoVersion"))
            implementation("cn.lalaki.central:central:1.2.5")
        }

        allOpen {
            annotation("com.ritense.valtimo.contract.annotation.AllOpen")
        }

        java {
            withSourcesJar()
            withJavadocJar()
        }

        fun findFirstExecutable(vararg candidates: String): String =
            candidates.firstOrNull { File(it).canExecute() } ?: candidates.last()

        val dockerPath = findFirstExecutable(
            // macOS (Intel Docker Desktop or Homebrew)
            "/usr/local/bin/docker",
            // macOS (Apple Silicon Homebrew)
            "/opt/homebrew/bin/docker",
            // Linux
            "/usr/bin/docker",
            // Fallback to PATH lookup
            "docker"
        )

        val composeV1Path = findFirstExecutable(
            "/usr/local/bin/docker-compose",
            "/opt/homebrew/bin/docker-compose",
            "/usr/bin/docker-compose",
            "docker-compose"
        )

        dockerCompose {
            projectNamePrefix = "example-"
            setProjectName("${rootProject.name}-${project.name}")

            // If you use Compose V2 (most setups today):
            // run "docker compose ..." via the docker CLI
            executable = dockerPath
            dockerExecutable = dockerPath
            // If your plugin exposes this flag, keep it on:
            // useComposeV2.set(true)

            // If you’re still on Compose V1, switch to:
            // executable = composeV1Path
            // dockerExecutable = dockerPath
        }

        tasks.test {
            useJUnitPlatform {
                excludeTags("integration")
            }
        }

        tasks.getByName<BootJar>("bootJar") {
            enabled = false
        }

        apply(from = "$rootDir/gradle/test.gradle.kts")
        apply(from = "$rootDir/gradle/plugin-properties.gradle.kts")
        val pluginProperties = extra["pluginProperties"] as Map<*, *>

        tasks.jar {
            enabled = true
            manifest {
                pluginProperties["pluginArtifactId"]?.let { attributes["Implementation-Title"] = it }
                pluginProperties["pluginVersion"]?.let { attributes["Implementation-Version"] = it }
            }
        }
    }
    if (project.path.startsWith(":backend") && project.name != "app" && project.name != "gradle" && project.name != "backend") {
        apply(from = "$rootDir/gradle/publishing.gradle")
    }
}

tasks.bootJar {
    enabled = false
}

println("Configuring has finished")
