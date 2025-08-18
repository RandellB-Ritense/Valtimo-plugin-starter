// Kotlin
package scaffold

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class ScaffoldBothTask : DefaultTask() {

    @get:Input @get:Optional var nameOpt: String? = null
    @get:Input @get:Optional var packageOpt: String? = null
    @get:Input @get:Optional var classPrefixOpt: String? = null
    @get:Input @get:Optional var pluginIdOpt: String? = null
    @get:Input @get:Optional var artifactOpt: String? = null
    @get:Input var failIfExists: Boolean = true

    @Option(option = "name", description = "Plugin name in CamelCase, e.g. DemoPlugin")
    fun setNameOption(v: String) { nameOpt = v }

    @Option(option = "package", description = "Base package (overrides auto), default com.ritense.<artifact>")
    fun setPackageOption(v: String) { packageOpt = v }

    @Option(option = "classPrefix", description = "Class prefix, defaults to --name")
    fun setClassPrefixOption(v: String) { classPrefixOpt = v }

    @Option(option = "pluginId", description = "Plugin ID, defaults to com.ritense.<artifact>")
    fun setPluginIdOption(v: String) { pluginIdOpt = v }

    @Option(option = "artifact", description = "Artifact (kebab-case), defaults from --name")
    fun setArtifactOption(v: String) { artifactOpt = v }

    @TaskAction
    fun run() {
        val pluginName = nameOpt
            ?: project.findProperty("scaffold.pluginName") as String?
            ?: error("Missing plugin name. Use: ./gradlew scaffoldPlugin --name DemoPlugin")

        val artifact = artifactOpt
            ?: project.findProperty("scaffold.artifact") as String?
            ?: toKebabFromCamel(pluginName)

        val pkg = packageOpt
            ?: project.findProperty("scaffold.package") as String?
            ?: "com.ritense.$artifact"

        val classPrefix = classPrefixOpt
            ?: project.findProperty("scaffold.classPrefix") as String?
            ?: pluginName

        val pluginId = pluginIdOpt
            ?: project.findProperty("scaffold.pluginId") as String?
            ?: "com.ritense.$artifact"

        failIfExists = (project.findProperty("scaffold.failIfExists") as String?)?.toBooleanStrictOrNull() ?: true

        val tokens = linkedMapOf(
            "__PACKAGE_NAME__" to pkg,
            "__PACKAGE_PATH__" to pkg.replace('.', '/'),
            "__PLUGIN_NAME__" to pluginName,
            "__PLUGIN_ID__" to pluginId,
            "__ARTIFACT_NAME__" to artifact,
            "__CLASS_PREFIX__" to classPrefix
        )

        val backendTemplate = (project.findProperty("scaffold.templateBackend") as String?) ?: "templates/backend-template"
        val backendOutput = resolvePathTokens(
            (project.findProperty("scaffold.outputBackend") as String?) ?: "backend/__ARTIFACT_NAME__", tokens
        )
        val frontendTemplate = (project.findProperty("scaffold.templateFrontend") as String?) ?: "templates/frontend-template"
        val frontendOutput = resolvePathTokens(
            (project.findProperty("scaffold.outputFrontend") as String?)
                ?: "frontend/projects/valtimo-plugins/__ARTIFACT_NAME__", tokens
        )

        scaffoldTree(
            templateDir = project.layout.projectDirectory.dir(backendTemplate).asFile.toPath(),
            outputDir = project.layout.projectDirectory.dir(backendOutput).asFile.toPath(),
            tokens = tokens,
            failIfExists = failIfExists
        )
        scaffoldTree(
            templateDir = project.layout.projectDirectory.dir(frontendTemplate).asFile.toPath(),
            outputDir = project.layout.projectDirectory.dir(frontendOutput).asFile.toPath(),
            tokens = tokens,
            failIfExists = failIfExists
        )

        val autoIncludeBackend = (project.findProperty("scaffold.autoIncludeBackend") as String?)
            ?.toBooleanStrictOrNull() ?: false
        if (autoIncludeBackend) {
            val includeLine = "include(\":backend:$artifact\")"
            val settings = project.layout.projectDirectory.file("settings.gradle.kts").asFile.toPath()
            if (Files.exists(settings)) {
                val current = settings.readText()
                if (!current.lineSequence().any { it.trim() == includeLine }) {
                    settings.writeText(current + System.lineSeparator() + includeLine + System.lineSeparator())
                    logger.lifecycle("Appended include to settings.gradle.kts: $includeLine")
                }
            }
        }

        logger.lifecycle("Scaffolded backend -> $backendOutput")
        logger.lifecycle("Scaffolded frontend -> $frontendOutput")
    }

    private fun toKebabFromCamel(s: String): String =
        s.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .replace(Regex("[\\s_]+"), "-")
            .lowercase(Locale.getDefault())

    private fun resolvePathTokens(path: String, tokens: Map<String, String>): String {
        var out = path
        tokens.keys.sortedByDescending { it.length }.forEach { t ->
            out = out.replace(t, tokens.getValue(t))
        }
        return out
    }

    private fun scaffoldTree(
        templateDir: Path,
        outputDir: Path,
        tokens: Map<String, String>,
        failIfExists: Boolean
    ) {
        require(templateDir.exists() && templateDir.isDirectory()) {
            "Template directory not found: $templateDir"
        }
        if (outputDir.exists()) {
            val nonEmpty = Files.walk(outputDir).use { it.anyMatch { p -> p != outputDir } }
            if (nonEmpty && failIfExists) {
                error("Output directory exists and is not empty: $outputDir (override with -Pscaffold.failIfExists=false)")
            }
        } else {
            outputDir.createDirectories()
        }

        val orderedTokens = tokens.keys.sortedByDescending { it.length }
        fun replaceTokens(input: String): String =
            orderedTokens.fold(input) { acc, key -> acc.replace(key, tokens.getValue(key)) }

        Files.walk(templateDir).use { stream ->
            stream.forEach { src ->
                if (src == templateDir) return@forEach
                val rel = templateDir.relativize(src)
                var renamedRel: Path = Path.of("")
                rel.iterator().forEachRemaining { seg ->
                    val replaced = replaceTokens(seg.toString())
                    renamedRel = if (renamedRel.toString().isEmpty()) Path.of(replaced) else renamedRel.resolve(replaced)
                }
                val dest = outputDir.resolve(renamedRel)
                if (src.isDirectory()) {
                    dest.createDirectories()
                } else {
                    dest.parent?.createDirectories()
                    val isText = isTextFile(src)
                    if (isText) {
                        val content = src.readText()
                        val replaced = replaceTokens(content)
                        dest.writeText(replaced)
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    private fun isTextFile(path: Path): Boolean {
        val name = path.name
        val dotIdx = name.lastIndexOf('.')
        if (dotIdx < 0) return true
        val ext = name.substring(dotIdx + 1).lowercase()
        return ext in setOf(
            "kt","kts","java","md","gradle","properties","yaml","yml","xml","json","txt","gitignore",
            "ts","tsx","js","mjs","cjs","html","css","scss","less","env","npmrc","nvmrc","eslintrc","prettierrc","svg"
        )
    }
}