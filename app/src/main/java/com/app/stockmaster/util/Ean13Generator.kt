package com.app.stockmaster.util

import kotlin.random.Random

object Ean13Generator {
    
    /**
     * Generates a random valid EAN-13 barcode.
     * Use a common prefix for internal items if needed (e.g., "20" for internal use)
     */
    fun generate(): String {
        // Generate first 12 digits
        // Using "20" as prefix for internal use (restricted circulation)
        val prefix = "20"
        val remainingDigits = (1..10).map { Random.nextInt(0, 10) }.joinToString("")
        val baseCode = prefix + remainingDigits
        
        return baseCode + calculateChecksum(baseCode)
    }

    /**
     * Calculates the EAN-13 checksum digit for a 12-digit string.
     */
    fun calculateChecksum(code12: String): Int {
        if (code12.length != 12) throw IllegalArgumentException("EAN-13 base must be 12 digits")
        
        var sum = 0
        for (i in 0 until 12) {
            val digit = code12[i].toString().toInt()
            // Even positions (1, 3, 5...) are multiplied by 1
            // Odd positions (0, 2, 4...) are multiplied by 1? 
            // Standard EAN-13: 
            // Position 1, 3, 5, 7, 9, 11 (index 0, 2, 4...) -> weight 1
            // Position 2, 4, 6, 8, 10, 12 (index 1, 3, 5...) -> weight 3
            sum += if (i % 2 == 0) digit else digit * 3
        }
        
        val mod = sum % 10
        return if (mod == 0) 0 else 10 - mod
    }

    /**
     * Validates if a string is a valid EAN-13 barcode.
     */
    fun isValid(code: String): Boolean {
        if (code.length != 13) return false
        if (!code.all { it.isDigit() }) return false
        
        val base = code.substring(0, 12)
        val check = code[12].toString().toInt()
        
        return calculateChecksum(base) == check
    }
}
