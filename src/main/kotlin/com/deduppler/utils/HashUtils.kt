package com.deduppler.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.Security

object HashUtils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun calculateBlake2bHash(file: File): String {
        val digest = MessageDigest.getInstance("BLAKE2B-512", "BC")
        
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
