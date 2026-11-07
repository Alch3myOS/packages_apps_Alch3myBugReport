package org.alch3my.bugreport

import java.security.SecureRandom

object Ids {
    // Omits confusing characters (0, 1, I, O)
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()
    private val PATTERN = Regex("BUG-[2-9A-HJ-NP-Z]{5}")

    fun next(): String = "BUG-" + (1..5).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    fun valid(id: String): Boolean = PATTERN.matches(id)
}