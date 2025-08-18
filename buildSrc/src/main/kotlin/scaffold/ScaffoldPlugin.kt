package scaffold

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class ScaffoldPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<ScaffoldBothTask>("scaffoldPlugin") {
            group = "scaffold"
            description = "Scaffold backend and frontend with --name; package defaults to com.ritense.<artifact>"
        }
    }
}