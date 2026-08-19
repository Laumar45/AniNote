package com.laumar.aninote.utils

/**
 * Result of parsing a .txt file for import.
 * @property animes Clean names ready to save to database
 * @property ignoredCount Lines discarded (empty, malformed, no name after trim)
 */
data class ParseResult(
    val animes: List<Pair<String, Int>>,
    val ignoredCount: Int
)

private val COUNT_SUFFIX_REGEX = Regex(",\\s*(\\d+)$")
private val LINE_NUMBER_PREFIX_REGEX = Regex("^\\d+\\.\\s*")

/**
 * Parse a .txt file content into a list of anime (name, vecesVisto) pairs.
 *
 * Format: one anime per line, optional "N. " prefix (ignored), optional ", N" suffix for view count.
 * If ", N" is present, N is extracted as vecesVisto and stripped from the name.
 * If no count is specified, vecesVisto defaults to 1.
 * All lines are trimmed. Empty lines and lines with no content after trimming are ignored.
 *
 * @param content Raw text file content
 * @return ParseResult with clean (name, vecesVisto) pairs and count of ignored lines
 */
fun parseTxtFile(content: String): ParseResult {
    val allLines = content.lines()
    val parsedAnimes = mutableListOf<Pair<String, Int>>()
    var ignoredCount = 0

    for (line in allLines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            ignoredCount++
            continue
        }

        val withoutPrefix = trimmed.replaceFirst(LINE_NUMBER_PREFIX_REGEX, "").trim()
        if (withoutPrefix.isEmpty()) {
            ignoredCount++
            continue
        }

        val match = COUNT_SUFFIX_REGEX.find(withoutPrefix)
        val (name, vecesVisto) = if (match != null) {
            val cleanName = withoutPrefix.substring(0, match.range.first).trim()
            val count = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            cleanName to count
        } else {
            withoutPrefix to 1
        }

        if (name.isEmpty()) {
            ignoredCount++
        } else {
            parsedAnimes.add(name to vecesVisto)
        }
    }

    return ParseResult(
        animes = parsedAnimes,
        ignoredCount = ignoredCount
    )
}

/**
 * Format a single anime line for .txt export with comma-separated view count if > 1.
 *
 * @param position Line number (1-based)
 * @param name Anime name
 * @param vecesVisto Times watched
 * @return Formatted line like "1. Name" or "1. Name, 3"
 */
fun formatLine(position: Int, name: String, vecesVisto: Int): String {
    return if (vecesVisto > 1) "$position. $name, $vecesVisto"
    else "$position. $name"
}

fun formatTxtLine(position: Int, name: String, vecesVisto: Int): String =
    formatLine(position, name, vecesVisto)

/**
 * Format a complete list of animes for .txt export.
 *
 * @param animes List of pairs (name, vecesVisto)
 * @return Complete file content
 */
fun formatTxtExport(animes: List<Pair<String, Int>>): String {
    return animes
        .mapIndexed { index, (name, vecesVisto) ->
            formatLine(index + 1, name, vecesVisto)
        }
        .joinToString("\n")
}
