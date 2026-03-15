package com.app.stockmaster.util

import android.util.Base64
import android.util.Log

object ImageUtils {
    /**
     * Decodes a Base64 string (optionally with data:image/... prefix) to a ByteArray.
     * Returns null if decoding fails or string is not a valid base64 image.
     */
    fun decodeBase64(base64String: String?): Any? {
        if (base64String == null) return null
        
        // If it's a standard URL, return it as is for Coil
        if (base64String.startsWith("http://") || base64String.startsWith("https://")) {
            return base64String
        }

        // Handle Base64 data URIs
        if (base64String.startsWith("data:image")) {
            try {
                val commaIndex = base64String.indexOf(",")
                if (commaIndex != -1) {
                    val pureBase64 = base64String.substring(commaIndex + 1)
                    return Base64.decode(pureBase64, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                Log.e("ImageUtils", "Error decoding base64 image", e)
            }
        }
        
        // Fallback for raw base64 without prefix (unlikely but possible)
        try {
            return Base64.decode(base64String, Base64.DEFAULT)
        } catch (e: Exception) {
            // Not base64, return original string (might be a placeholder or local path)
            return base64String
        }
    }
}
