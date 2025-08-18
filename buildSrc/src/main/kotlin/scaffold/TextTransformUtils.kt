package scaffold

import java.util.Locale

object TextTransformUtils {
    fun toFlatCase(input: String): String {
        return input
            // Remove separators (dot, dash, slash, underscore, space)
            .replace(Regex("[._\\-/\\s]+"), "")
            // Insert a marker before capitals, then lowercase everything
            .replace(Regex("([a-z0-9])([A-Z])"), "$1$2")
            .lowercase()
    }

    fun normalizeToWords(input: String): String {
        return input
            // Replace separators (dot, dash, slash, underscore) with space
            .replace(Regex("[._\\-/]+"), " ")
            // Insert space before capital letters (except at start)
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            // Normalize spacing and lowercase
            .trim()
            .lowercase()
    }

    fun toKebabCase(input: String): String {
        return input
            // split camelCase and PascalCase
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            // split ALLCAPS followed by normal word (e.g. "HTMLParser" → "HTML-Parser")
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1-$2")
            // normalize underscores, spaces, multiple dashes into single dash
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("-+"), "-")
            // lowercase everything
            .lowercase(Locale.getDefault())
            // trim accidental leading/trailing dashes
            .trim('-')
    }
    fun toPascalCase(input: String): String {
        return input
            .split('_', '-', ' ')
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    fun toCamelCase(input: String): String {
        val parts = input
            .split('_', '-', ' ')
            .filter { it.isNotBlank() }
            .map { it.lowercase() }

        return parts.first() + parts.drop(1)
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}