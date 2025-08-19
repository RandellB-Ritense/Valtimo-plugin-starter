// Kotlin
package scaffold

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import scaffold.helpers.ScaffoldFileTree
import scaffold.helpers.TextTransform
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class ScaffoldBothTask : DefaultTask(){

    @get:Input @get:Optional var nameOpt: String? = null
    @get:Input @get:Optional var packageOpt: String? = null
    @get:Input @get:Optional var classPrefixOpt: String? = null
    @get:Input @get:Optional var functionPrefixOpt: String? = null
    @get:Input @get:Optional var artifactOpt: String? = null
    @get:Input var failIfExists: Boolean = true

    @Option(option = "name", description = "Plugin name in CamelCase, e.g. DemoPlugin")
    fun setNameOption(v: String) { nameOpt = v }

    @Option(option = "package", description = "Base package (overrides auto), default com.ritense.<artifact>")
    fun setPackageOption(v: String) { packageOpt = v }

    @Option(option = "classPrefix", description = "Class prefix, defaults to --name")
    fun setClassPrefixOption(v: String) { classPrefixOpt = v }

    @Option(option = "functionPrefix", description = "Function prefix, defaults to --name")
    fun setFunctionPrefixOption(v: String) { functionPrefixOpt = v }


    @Option(option = "artifact", description = "Artifact (kebab-case), defaults from --name")
    fun setArtifactOption(v: String) { artifactOpt = v }

    @TaskAction
    fun run() {
        val pluginName = TextTransform.normalizeToWords(nameOpt
            ?: project.findProperty("scaffold.pluginName") as String?
            ?: error("Missing plugin name. Use: ./gradlew scaffoldPlugin --name DemoPlugin")
                )

        val artifact = artifactOpt
            ?: project.findProperty("scaffold.artifact") as String?
            ?: TextTransform.toKebabCase(pluginName)

        val pkg = packageOpt
            ?: project.findProperty("scaffold.package") as String?
            ?: "com.ritense.${TextTransform.toFlatCase(pluginName)}"

        val classPrefix = classPrefixOpt
            ?: project.findProperty("scaffold.classPrefix") as String?
            ?: TextTransform.toPascalCase(pluginName)

        val functionPrefix = functionPrefixOpt
            ?: project.findProperty("scaffold.functionPrefix") as String?
            ?: TextTransform.toCamelCase(pluginName)

        failIfExists = (project.findProperty("scaffold.failIfExists") as String?)?.toBooleanStrictOrNull() ?: true

        val tokens = linkedMapOf(
            "__PACKAGE_NAME__" to pkg,
            "__PACKAGE_PATH__" to pkg.replace('.', '/'),
            "__PLUGIN_NAME__" to pluginName,
            "__ARTIFACT_NAME__" to artifact,
            "__CLASS_PREFIX__" to classPrefix,
            "__FUNCTION_PREFIX__" to functionPrefix
        )

        val backendTemplate = (project.findProperty("scaffold.templateBackend") as String?) ?: "templates/backend-template"
        val backendOutput = ScaffoldFileTree.resolvePathTokens(
            (project.findProperty("scaffold.outputBackend") as String?) ?: "backend/__ARTIFACT_NAME__", tokens
        )
        val frontendTemplate = (project.findProperty("scaffold.templateFrontend") as String?) ?: "templates/frontend-template"
        val frontendOutput = ScaffoldFileTree.resolvePathTokens(
            (project.findProperty("scaffold.outputFrontend") as String?)
                ?: "frontend/projects/valtimo-plugins/__ARTIFACT_NAME__", tokens
        )

        ScaffoldFileTree.scaffoldTree(
            templateDir = project.layout.projectDirectory.dir(backendTemplate).asFile.toPath(),
            outputDir = project.layout.projectDirectory.dir(backendOutput).asFile.toPath(),
            tokens = tokens,
            failIfExists = failIfExists
        )
        ScaffoldFileTree.scaffoldTree(
            templateDir = project.layout.projectDirectory.dir(frontendTemplate).asFile.toPath(),
            outputDir = project.layout.projectDirectory.dir(frontendOutput).asFile.toPath(),
            tokens = tokens,
            failIfExists = failIfExists
        )

//        // Register backend plugin
//        val includeLine = "include(\":backend:$artifact\")"
//        val settings = project.layout.projectDirectory.file("settings.gradle.kts").asFile.toPath()
//        if (Files.exists(settings)) {
//            val current = settings.readText()
//            if (!current.lineSequence().any { it.trim() == includeLine }) {
//                settings.writeText(current + System.lineSeparator() + includeLine + System.lineSeparator())
//                logger.lifecycle("Appended include to settings.gradle.kts: $includeLine")
//            }
//        }

        ScaffoldPluginRegistrarBackend.addBackendModule(project.layout.projectDirectory.file("settings.gradle.kts").asFile.toPath(), artifact)

        // Register frontend plugin
        ScaffoldPluginRegistrarFrontend.registerAll(artifact, classPrefix, functionPrefix)

        logger.lifecycle("Scaffolded backend -> $backendOutput")
        logger.lifecycle("Scaffolded frontend -> $frontendOutput")
        logger.lifecycle("Resolved scaffold tokens:")
        tokens.forEach { (k, v) ->
            logger.lifecycle("  $k -> $v")
        }
    }
}