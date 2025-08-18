// Kotlin
package scaffold

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class ScaffoldTask : DefaultTask() {

    @Input lateinit var templateDirStr: String
    @Input lateinit var outputDirStr: String
    @Input lateinit var tokens: Map<String, String>
    @Input var failIfExists: Boolean = true
    @Optional @Input var autoIncludeBackend: Boolean = false

    @TaskAction
    fun run() {
        val templateDir = project.layout.projectDirectory.dir(templateDirStr).asFile.toPath()
        val outputDir = project.layout.projectDirectory.dir(outputDirStr).asFile.toPath()

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

                // Replace tokens per path segment to remain cross-platform
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

        logger.lifecycle("Scaffolded to: $outputDir")

        if (autoIncludeBackend && outputDirStr.startsWith("backend/")) {
            val artifact = tokens["__ARTIFACT_NAME__"] ?: return
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
    }

    private fun isTextFile(path: Path): Boolean {
        val name = path.name
        val dotIdx = name.lastIndexOf('.')
        if (dotIdx < 0) return true // no extension; assume text
        val ext = name.substring(dotIdx + 1).lowercase()
        return ext in setOf(
            "kt","kts","java","md","gradle","properties","yaml","yml","xml","json","txt","gitignore",
            "ts","tsx","js","mjs","cjs","html","css","scss","less","env","npmrc","nvmrc","eslintrc","prettierrc","svg"
        )
    }
}