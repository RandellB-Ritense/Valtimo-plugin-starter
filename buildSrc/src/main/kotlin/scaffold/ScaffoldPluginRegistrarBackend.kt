package scaffold

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ScaffoldPluginRegistrarBackend {

    fun addBackendModule(settingsGradleKts: Path, artifact: String) {
        if (!Files.exists(settingsGradleKts)) return

        // 1) settings.gradle.kts → include("backend:<artifact>")
        val module = "backend:$artifact"
        val raw = settingsGradleKts.readText()

        if (!raw.contains("\"$module\"") && !raw.contains("'$module'")) {
            val rx = Regex("""include\s*\(([\s\S]*?)\)""")
            val match = rx.find(raw)
            if (match != null) {
                val inside = match.groupValues[1]
                val trimmed = inside.trim()
                val needsComma = trimmed.isNotEmpty() && !trimmed.trimEnd().endsWith(",")

                val indent = Regex("""\r?\n([ \t]+)""").find(inside)?.groupValues?.get(1) ?: "    "
                val insertion = buildString {
                    if (needsComma) append(",")
                    append("\n").append(indent).append('"').append(module).append("\",\n")
                }

                val replacement = "include(" + inside + insertion + ")"
                val updated = raw.replaceRange(match.range, replacement)
                settingsGradleKts.writeText(updated)
            }
        }

        // 2) backend/app/build.gradle.kts → dependencies { implementation(project(":backend:<artifact>")) }
        val repoRoot = settingsGradleKts.parent
        val appBuildGradle = repoRoot.resolve("backend/app/build.gradle.kts")
        if (Files.exists(appBuildGradle)) {
            var code = appBuildGradle.readText()

            val depLine = """implementation(project(":backend:$artifact"))"""

            // already present? bail
            if (!code.contains(depLine)) {
                val depsHead = Regex("""(?m)^\s*dependencies\s*\{""").find(code)
                if (depsHead != null) {
                    val openIdx = code.indexOf('{', depsHead.range.last)
                    val closeIdx = findMatchingBrace(code, openIdx) ?: run {
                        // fallback: append a new block at EOF
                        code += "\n\ndependencies {\n    $depLine\n}\n"
                        appBuildGradle.writeText(code)
                        return
                    }

                    val before = code.substring(0, openIdx + 1)
                    val body = code.substring(openIdx + 1, closeIdx)
                    val after = code.substring(closeIdx)

                    val indent = inferIndent(body, default = "    ")
                    val insertion = "\n$indent$depLine\n"

                    val newBody = if (body.trim().isEmpty()) insertion.trimStart() else body + insertion
                    code = before + newBody + after
                } else {
                    // no dependencies block → create one
                    code += "\n\ndependencies {\n    $depLine\n}\n"
                }

                appBuildGradle.writeText(code)
            }
        }
    }

    // --- helpers ---

    private fun findMatchingBrace(text: String, openIdx: Int): Int? {
        var depth = 0
        var i = openIdx
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
                '"', '\'' -> {
                    // skip quoted segments to avoid counting braces inside strings
                    val q = text[i]; i++
                    while (i < text.length && text[i] != q) {
                        if (text[i] == '\\') i++
                        i++
                    }
                }
            }
            i++
        }
        return null
    }

    private fun inferIndent(block: String, default: String): String {
        val firstLine = block.lines().firstOrNull { it.isNotBlank() } ?: return default
        val ws = firstLine.takeWhile { it == ' ' || it == '\t' }
        return if (ws.isNotEmpty()) ws else default
    }
}