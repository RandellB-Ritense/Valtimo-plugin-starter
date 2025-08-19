package scaffold.utlis

import java.util.Locale

object TextTransform {
    fun toFlatCase(input: String): String {
        return input
            .replace(Regex("[._\\-/\\s]+"), "")
            .replace(Regex("([a-z0-9])([A-Z])"), "$1$2")
            .lowercase()
    }

    fun normalizeToWords(input: String): String {
        return input
            .replace(Regex("[._\\-/]+"), " ")
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .trim()
            .lowercase()
    }

    fun toKebabCase(input: String): String {
        return input
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1-$2")
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("-+"), "-")
            .lowercase(Locale.getDefault())
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