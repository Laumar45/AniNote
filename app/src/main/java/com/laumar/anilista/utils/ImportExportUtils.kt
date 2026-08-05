package com.laumar.anilista.utils

/**
 * Result of parsing a .txt file for import.
 * @property animes Clean names ready to save to database
 * @property ignoredCount Lines discarded (empty, malformed, no name after trim)
 */
data class ParseResult(
    val animes: List<String>,
    val ignoredCount: Int
)

/**
 * Parse a .txt file content into a list of anime names.
 *
 * Format: one anime per line, optional "N. " prefix (ignored), optional " xN" suffix (treated as part of name).
 * All lines are trimmed. Empty lines and lines with no content after trimming are ignored.
 *
 * @param content Raw text file content
 * @property animes Clean names ready to save
 * @property ignoredCount Number of lines that were discarded
 */
fun parseTxtFile(content: String): ParseResult {
    val allLines = content.lines()
    val animes = allLines
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.replaceFirst(Regex("^\\d+\\.\\s*"), "").trim() }
        .filter { it.isNotEmpty() }
    return ParseResult(
        animes = animes,
        ignoredCount = allLines.size - animes.size
    )
}

/**
 * Format a single anime line for .txt export.
 *
 * Rules:
 * - vecesVisto == 1 → no suffix
 * - vecesVisto > 1 → add " xN"
 * - Strip any existing " xN" suffix before applying current vecesVisto
 *
 * @param position Line number (1-based)
 * @param name Anime name
 * @param vecesVisto Times watched
 * @return Formatted line like "1. Name" or "1. Name x2"
 */
fun formatTxtLine(position: Int, name: String, vecesVisto: Int): String {
    val cleanName = name.replace(Regex("\\s+x\\d+$"), "")
    val base = "$position. $cleanName"
    return if (vecesVisto > 1) "$base x$vecesVisto" else base
}

/**
 * Format a complete list of animes for .txt export.
 *
 * @param animes List of pairs (name, vecesVisto)
 * @return Complete file content
 */
fun formatTxtExport(animes: List<Pair<String, Int>>): String {
    return animes
        .mapIndexed { index, (name, vecesVisto) ->
            formatTxtLine(index + 1, name, vecesVisto)
        }
        .joinToString("\n")
}
