package scaffold.helpers

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.use

object ScaffoldFileTree {
    fun scaffoldTree(
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

    fun resolvePathTokens(path: String, tokens: Map<String, String>): String {
        var out = path
        tokens.keys.sortedByDescending { it.length }.forEach { t ->
            out = out.replace(t, tokens.getValue(t))
        }
        return out
    }

    fun isTextFile(path: Path): Boolean {
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