package org.example.equipment.util

/** Shared literal search normalization; never changes persisted names or opaque identities. */
fun normalizeEquipmentSearchText(value: String): String = value
    .replace(Regex("[\\p{javaWhitespace}\\p{Z}]+"), " ")
    .trim()
    .map { if (it in 'A'..'Z') it + ('a' - 'A') else it }
    .joinToString("")
