package scaffold

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ScaffoldPluginRegistrarBackend {
    fun addBackendModule(settingsGradleKts: Path, artifact: String) {
        if (!Files.exists(settingsGradleKts)) return
        val module = "backend:$artifact"
        val raw = settingsGradleKts.readText()

        // already present? bail
        if (raw.contains("\"$module\"") || raw.contains("'$module'")) return

        // match the FIRST include(...) block
        val rx = Regex("""include\s*\(([\s\S]*?)\)""")
        val match = rx.find(raw) ?: return

        val inside = match.groupValues[1]
        val trimmed = inside.trim()
        val needsComma = trimmed.isNotEmpty() && !trimmed.trimEnd().endsWith(",")

        // infer indentation from existing entries (fallback: 4 spaces)
        val indent = Regex("""\r?\n([ \t]+)""").find(inside)?.groupValues?.get(1) ?: "    "

        val insertion = buildString {
            if (needsComma) append(",")
            append("\n").append(indent).append('"').append(module).append("\",\n")
        }

        // rebuild only the matched include(...) block
        val replacement = "include(" + inside + insertion + ")"
        val updated = raw.replaceRange(match.range, replacement)

        settingsGradleKts.writeText(updated)
    }
}